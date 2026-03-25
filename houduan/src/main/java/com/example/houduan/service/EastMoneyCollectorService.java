package com.example.houduan.service;

import com.example.houduan.entity.IndustryBaseResponseEntity;
import com.example.houduan.entity.StockPoolResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class EastMoneyCollectorService {

    private final EastMoneyApiClient eastMoneyApiClient;
    private final EastMoneyStorageService eastMoneyStorageService;
    private final CrawlJobLogService crawlJobLogService;
    private final int maxStockPoolPages;

    public EastMoneyCollectorService(
        EastMoneyApiClient eastMoneyApiClient,
        EastMoneyStorageService eastMoneyStorageService,
        CrawlJobLogService crawlJobLogService,
        @Value("${collector.max-stock-pool-pages:1}") int maxStockPoolPages
    ) {
        this.eastMoneyApiClient = eastMoneyApiClient;
        this.eastMoneyStorageService = eastMoneyStorageService;
        this.crawlJobLogService = crawlJobLogService;
        this.maxStockPoolPages = maxStockPoolPages;
    }

    @Scheduled(cron = "${collector.industry-base.cron}")
    public void collectIndustryBase() {
        runJob("industry-base", null, () -> {
            IndustryBaseResponseEntity saved = eastMoneyStorageService.saveIndustryBase(eastMoneyApiClient.fetchIndustryBase());
            return saved.getItemCount();
        });
    }

    @Scheduled(cron = "${collector.stock-pool.cron}")
    public void collectStockPool() {
        for (int pageNo = 1; pageNo <= maxStockPoolPages; pageNo++) {
            final int currentPage = pageNo;
            runJob("stock-pool", String.valueOf(currentPage), () -> {
                StockPoolResponseEntity saved = eastMoneyStorageService.saveStockPool(currentPage, eastMoneyApiClient.fetchStockPool(currentPage));
                return saved.getItemCount();
            });
        }
    }

    @Scheduled(cron = "${collector.industry-kline.cron}")
    public void collectIndustryKlines() {
        List<String> industryCodes = eastMoneyStorageService.findLatestIndustryBase()
            .map(IndustryBaseResponseEntity::getRawJson)
            .map(eastMoneyStorageService::extractIndustryCodes)
            .orElse(Collections.emptyList());

        for (String industryCode : industryCodes) {
            runJob("industry-kline", industryCode, () -> {
                eastMoneyStorageService.saveIndustryKline(industryCode, eastMoneyApiClient.fetchIndustryKline(industryCode));
                return null;
            });
        }
    }

    @Scheduled(cron = "${collector.stock-detail.cron}")
    public void collectStockDetails() {
        for (int pageNo = 1; pageNo <= maxStockPoolPages; pageNo++) {
            List<String> stockCodes = eastMoneyStorageService.findLatestStockPool(pageNo)
                .map(StockPoolResponseEntity::getRawJson)
                .map(eastMoneyStorageService::extractStockCodes)
                .orElse(Collections.emptyList());

            for (String stockCode : stockCodes) {
                runJob("stock-real", stockCode, () -> {
                    eastMoneyStorageService.saveStockReal(stockCode, eastMoneyApiClient.fetchStockReal(stockCode));
                    return null;
                });
                runJob("stock-kline", stockCode, () -> {
                    eastMoneyStorageService.saveStockKline(stockCode, eastMoneyApiClient.fetchStockKline(stockCode));
                    return null;
                });
            }
        }
    }

    private void runJob(String jobName, String targetKey, CrawlAction action) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            Integer recordCount = action.run();
            crawlJobLogService.log(jobName, targetKey, startedAt, true, recordCount, "OK");
        } catch (Exception e) {
            crawlJobLogService.log(jobName, targetKey, startedAt, false, null, truncate(e.getMessage()));
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= 1900 ? message : message.substring(0, 1900);
    }

    @FunctionalInterface
    private interface CrawlAction {
        Integer run();
    }
}
