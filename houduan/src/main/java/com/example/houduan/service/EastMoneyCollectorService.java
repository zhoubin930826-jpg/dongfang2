package com.example.houduan.service;

import com.example.houduan.entity.IndustryBaseResponseEntity;
import com.example.houduan.entity.StockPoolResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EastMoneyCollectorService {

    private final EastMoneyApiClient eastMoneyApiClient;
    private final EastMoneyStorageService eastMoneyStorageService;
    private final CrawlJobLogService crawlJobLogService;
    private final int maxStockPoolPages;
    private final int stockPoolPageSize;
    private final int stockPoolPauseAfterPages;
    private final long stockPoolPauseMs;
    private final int stockDetailPauseAfterSymbols;
    private final long stockDetailPauseMs;
    private final Map<String, AtomicBoolean> runningJobs = new ConcurrentHashMap<>();

    public EastMoneyCollectorService(
        EastMoneyApiClient eastMoneyApiClient,
        EastMoneyStorageService eastMoneyStorageService,
        CrawlJobLogService crawlJobLogService,
        @Value("${collector.max-stock-pool-pages:1}") int maxStockPoolPages,
        @Value("${collector.stock-pool.page-size:100}") int stockPoolPageSize,
        @Value("${collector.stock-pool.pause-after-pages:10}") int stockPoolPauseAfterPages,
        @Value("${collector.stock-pool.pause-ms:5000}") long stockPoolPauseMs,
        @Value("${collector.stock-detail.pause-after-symbols:40}") int stockDetailPauseAfterSymbols,
        @Value("${collector.stock-detail.pause-ms:15000}") long stockDetailPauseMs
    ) {
        this.eastMoneyApiClient = eastMoneyApiClient;
        this.eastMoneyStorageService = eastMoneyStorageService;
        this.crawlJobLogService = crawlJobLogService;
        this.maxStockPoolPages = maxStockPoolPages;
        this.stockPoolPageSize = stockPoolPageSize;
        this.stockPoolPauseAfterPages = stockPoolPauseAfterPages;
        this.stockPoolPauseMs = stockPoolPauseMs;
        this.stockDetailPauseAfterSymbols = stockDetailPauseAfterSymbols;
        this.stockDetailPauseMs = stockDetailPauseMs;
    }

    @Scheduled(cron = "${collector.industry-base.cron}")
    public void collectIndustryBase() {
        runScheduledExclusively("industry-base-schedule", () ->
            runJob("industry-base", null, () -> {
                IndustryBaseResponseEntity saved = eastMoneyStorageService.saveIndustryBase(eastMoneyApiClient.fetchIndustryBase());
                return saved.getItemCount();
            })
        );
    }

    @Scheduled(cron = "${collector.stock-pool.cron}")
    public void collectStockPool() {
        runScheduledExclusively("stock-pool-schedule", this::doCollectStockPool);
    }

    @Scheduled(cron = "${collector.industry-kline.cron}")
    public void collectIndustryKlines() {
        runScheduledExclusively("industry-kline-schedule", this::doCollectIndustryKlines);
    }

    @Scheduled(cron = "${collector.stock-detail.cron}")
    public void collectStockDetails() {
        runScheduledExclusively("stock-detail-schedule", this::doCollectStockDetails);
    }

    private void doCollectStockPool() {
        LocalDateTime startedAt = LocalDateTime.now();
        String firstPageRawJson;
        try {
            firstPageRawJson = eastMoneyApiClient.fetchStockPool(1);
            StockPoolResponseEntity firstPageSaved = eastMoneyStorageService.saveStockPool(1, firstPageRawJson);
            crawlJobLogService.log("stock-pool", "1", startedAt, true, firstPageSaved.getItemCount(), "OK");
            pauseIfNeeded("stock-pool", 1, stockPoolPauseAfterPages, stockPoolPauseMs);
        } catch (Exception e) {
            crawlJobLogService.log("stock-pool", "1", startedAt, false, null, truncate(e.getMessage()));
            return;
        }

        int totalPages = resolvePageLimit(firstPageRawJson);
        for (int pageNo = 2; pageNo <= totalPages; pageNo++) {
            final int currentPage = pageNo;
            runJob("stock-pool", String.valueOf(currentPage), () -> {
                StockPoolResponseEntity saved = eastMoneyStorageService.saveStockPool(currentPage, eastMoneyApiClient.fetchStockPool(currentPage));
                pauseIfNeeded("stock-pool", currentPage, stockPoolPauseAfterPages, stockPoolPauseMs);
                return saved.getItemCount();
            });
        }
    }

    private void doCollectIndustryKlines() {
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

    private void doCollectStockDetails() {
        Map<String, EastMoneyStorageService.StockTarget> stockTargetsByCode = new LinkedHashMap<>();
        Optional<StockPoolResponseEntity> referencePage = eastMoneyStorageService.findLatestStockPool(1);
        int pageLimit = referencePage
            .map(entity -> resolvePageLimit(entity.getRawJson()))
            .orElse(0);
        LocalDateTime referenceFetchedAt = referencePage.map(StockPoolResponseEntity::getFetchedAt).orElse(null);

        for (int pageNo = 1; pageNo <= pageLimit; pageNo++) {
            List<EastMoneyStorageService.StockTarget> stockTargets = eastMoneyStorageService.findLatestStockPoolAtOrBefore(pageNo, referenceFetchedAt)
                .map(StockPoolResponseEntity::getRawJson)
                .map(eastMoneyStorageService::extractStockTargets)
                .orElse(Collections.emptyList());

            for (EastMoneyStorageService.StockTarget stockTarget : stockTargets) {
                stockTargetsByCode.putIfAbsent(stockTarget.stockCode(), stockTarget);
            }
        }

        int processedSymbolCount = 0;
        for (EastMoneyStorageService.StockTarget stockTarget : stockTargetsByCode.values()) {
            runJob("stock-real", stockTarget.stockCode(), () -> {
                    eastMoneyStorageService.saveStockReal(
                        stockTarget.stockCode(),
                        eastMoneyApiClient.fetchStockReal(stockTarget.stockCode(), stockTarget.market())
                    );
                    return null;
                });
            runJob("stock-kline", stockTarget.stockCode(), () -> {
                    eastMoneyStorageService.saveStockKline(
                        stockTarget.stockCode(),
                        eastMoneyApiClient.fetchStockKline(stockTarget.stockCode(), stockTarget.market())
                    );
                    return null;
                });
            processedSymbolCount++;
            pauseIfNeeded("stock-detail", processedSymbolCount, stockDetailPauseAfterSymbols, stockDetailPauseMs);
        }
    }

    private int resolvePageLimit(String firstPageRawJson) {
        int totalPages = eastMoneyStorageService.extractTotalPages(firstPageRawJson, stockPoolPageSize);
        if (totalPages <= 0) {
            totalPages = 1;
        }
        if (maxStockPoolPages > 0) {
            return Math.min(totalPages, maxStockPoolPages);
        }
        return totalPages;
    }

    private void runScheduledExclusively(String jobKey, Runnable action) {
        AtomicBoolean running = runningJobs.computeIfAbsent(jobKey, key -> new AtomicBoolean(false));
        if (!running.compareAndSet(false, true)) {
            System.out.println("跳过调度任务，上一轮尚未完成: " + jobKey);
            return;
        }

        try {
            action.run();
        } finally {
            running.set(false);
        }
    }

    private void pauseIfNeeded(String jobName, int processedCount, int pauseAfterCount, long pauseMs) {
        if (pauseAfterCount <= 0 || pauseMs <= 0) {
            return;
        }
        if (processedCount > 0 && processedCount % pauseAfterCount == 0) {
            System.out.println("任务 " + jobName + " 已处理 " + processedCount + " 项，主动休息 " + pauseMs + "ms 以降低风控概率");
            sleepQuietly(pauseMs);
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

    private void sleepQuietly(long sleepMs) {
        if (sleepMs <= 0) {
            return;
        }
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface CrawlAction {
        Integer run();
    }
}
