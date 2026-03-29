package com.example.houduan.service;

import com.example.houduan.entity.IndustryBaseResponseEntity;
import com.example.houduan.entity.IndustryKlineResponseEntity;
import com.example.houduan.entity.StockKlineResponseEntity;
import com.example.houduan.entity.StockPoolResponseEntity;
import com.example.houduan.entity.StockRealResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Service
public class EastMoneyQueryService {

    private final ObjectMapper objectMapper;
    private final EastMoneyApiClient eastMoneyApiClient;
    private final EastMoneyStorageService eastMoneyStorageService;
    private final Duration freshnessWindow;
    private final Set<String> refreshingKeys = ConcurrentHashMap.newKeySet();
    private final ExecutorService refreshExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "eastmoney-query-refresh");
        thread.setDaemon(true);
        return thread;
    });

    public EastMoneyQueryService(
        ObjectMapper objectMapper,
        EastMoneyApiClient eastMoneyApiClient,
        EastMoneyStorageService eastMoneyStorageService,
        @Value("${collector.query-freshness-minutes:60}") long freshnessMinutes
    ) {
        this.objectMapper = objectMapper;
        this.eastMoneyApiClient = eastMoneyApiClient;
        this.eastMoneyStorageService = eastMoneyStorageService;
        this.freshnessWindow = Duration.ofMinutes(freshnessMinutes);
    }

    public String getIndustryBase() {
        Optional<CachedPayload> existing = eastMoneyStorageService.findLatestIndustryBase()
            .map(entity -> new CachedPayload(entity.getRawJson(), entity.getFetchedAt()));

        return resolveWithStaleWhileRefresh(
            "industry-base",
            existing,
            () -> {
                IndustryBaseResponseEntity saved = eastMoneyStorageService.saveIndustryBase(
                    eastMoneyApiClient.fetchIndustryBase()
                );
                return new CachedPayload(saved.getRawJson(), saved.getFetchedAt());
            }
        );
    }

    public String getIndustryKline(String industryCode) {
        Optional<CachedPayload> existing = eastMoneyStorageService.findLatestIndustryKline(industryCode)
            .map(entity -> new CachedPayload(entity.getRawJson(), entity.getFetchedAt()));

        return resolveWithStaleWhileRefresh(
            "industry-kline:" + industryCode,
            existing,
            () -> {
                IndustryKlineResponseEntity saved = eastMoneyStorageService.saveIndustryKline(
                    industryCode,
                    eastMoneyApiClient.fetchIndustryKline(industryCode)
                );
                return new CachedPayload(saved.getRawJson(), saved.getFetchedAt());
            }
        );
    }

    public String getStockReal(String stockCode, Integer market) {
        Optional<CachedPayload> existing = eastMoneyStorageService.findLatestStockReal(stockCode)
            .map(entity -> new CachedPayload(entity.getRawJson(), entity.getFetchedAt()));

        return resolveWithStaleWhileRefresh(
            "stock-real:" + stockCode,
            existing,
            () -> {
                StockRealResponseEntity saved = eastMoneyStorageService.saveStockReal(
                    stockCode,
                    eastMoneyApiClient.fetchStockReal(stockCode, market)
                );
                return new CachedPayload(saved.getRawJson(), saved.getFetchedAt());
            }
        );
    }

    public String getStockKline(String stockCode, Integer market) {
        Optional<CachedPayload> existing = eastMoneyStorageService.findLatestStockKline(stockCode)
            .map(entity -> new CachedPayload(entity.getRawJson(), entity.getFetchedAt()));

        return resolveWithStaleWhileRefresh(
            "stock-kline:" + stockCode,
            existing,
            () -> {
                StockKlineResponseEntity saved = eastMoneyStorageService.saveStockKline(
                    stockCode,
                    eastMoneyApiClient.fetchStockKline(stockCode, market)
                );
                return new CachedPayload(saved.getRawJson(), saved.getFetchedAt());
            }
        );
    }

    public String getStockPool(int pageNo) {
        Optional<CachedPayload> existing = eastMoneyStorageService.findLatestStockPool(pageNo)
            .map(entity -> new CachedPayload(entity.getRawJson(), entity.getFetchedAt()));

        return resolveWithStaleWhileRefresh(
            "stock-pool:" + pageNo,
            existing,
            () -> {
                StockPoolResponseEntity saved = eastMoneyStorageService.saveStockPool(
                    pageNo,
                    eastMoneyApiClient.fetchStockPool(pageNo)
                );
                return new CachedPayload(saved.getRawJson(), saved.getFetchedAt());
            }
        );
    }

    private boolean isFresh(Optional<LocalDateTime> fetchedAt) {
        return fetchedAt
            .map(value -> value.isAfter(LocalDateTime.now().minus(freshnessWindow)))
            .orElse(false);
    }

    private String resolveWithStaleWhileRefresh(
        String cacheKey,
        Optional<CachedPayload> existing,
        Supplier<CachedPayload> refreshSupplier
    ) {
        if (existing.isPresent()) {
            CachedPayload cachedPayload = existing.get();
            if (!isFresh(Optional.ofNullable(cachedPayload.fetchedAt()))) {
                triggerBackgroundRefresh(cacheKey, refreshSupplier);
            }
            return attachMeta(cachedPayload);
        }

        System.out.println("缓存未命中，同步抓取: " + cacheKey);
        return attachMeta(refreshSupplier.get());
    }

    private void triggerBackgroundRefresh(String cacheKey, Supplier<CachedPayload> refreshSupplier) {
        if (!refreshingKeys.add(cacheKey)) {
            return;
        }

        System.out.println("缓存已过期，后台刷新: " + cacheKey);
        refreshExecutor.submit(() -> {
            try {
                refreshSupplier.get();
            } catch (Exception ex) {
                System.err.println("后台刷新失败: " + cacheKey + " - " + ex.getMessage());
            } finally {
                refreshingKeys.remove(cacheKey);
            }
        });
    }

    private String attachMeta(CachedPayload payload) {
        try {
            JsonNode root = objectMapper.readTree(payload.rawJson());
            if (root instanceof ObjectNode objectNode) {
                ObjectNode metaNode = objectNode.putObject("_meta");
                if (payload.fetchedAt() != null) {
                    metaNode.put("fetchedAt", payload.fetchedAt().toString());
                }
                metaNode.put("isFresh", isFresh(Optional.ofNullable(payload.fetchedAt())));
                return objectMapper.writeValueAsString(objectNode);
            }
        } catch (Exception ex) {
            System.err.println("注入 _meta 失败: " + ex.getMessage());
        }
        return payload.rawJson();
    }

    @PreDestroy
    public void shutdownRefreshExecutor() {
        refreshExecutor.shutdownNow();
    }

    private record CachedPayload(String rawJson, LocalDateTime fetchedAt) {
    }
}
