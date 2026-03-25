package com.example.houduan.service;

import com.example.houduan.entity.IndustryBaseResponseEntity;
import com.example.houduan.entity.IndustryKlineResponseEntity;
import com.example.houduan.entity.StockKlineResponseEntity;
import com.example.houduan.entity.StockPoolResponseEntity;
import com.example.houduan.entity.StockRealResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EastMoneyQueryService {

    private final EastMoneyApiClient eastMoneyApiClient;
    private final EastMoneyStorageService eastMoneyStorageService;
    private final Duration freshnessWindow;

    public EastMoneyQueryService(
        EastMoneyApiClient eastMoneyApiClient,
        EastMoneyStorageService eastMoneyStorageService,
        @Value("${collector.query-freshness-minutes:60}") long freshnessMinutes
    ) {
        this.eastMoneyApiClient = eastMoneyApiClient;
        this.eastMoneyStorageService = eastMoneyStorageService;
        this.freshnessWindow = Duration.ofMinutes(freshnessMinutes);
    }

    public String getIndustryBase() {
        Optional<IndustryBaseResponseEntity> existing = eastMoneyStorageService.findLatestIndustryBase();
        if (isFresh(existing.map(IndustryBaseResponseEntity::getFetchedAt))) {
            return existing.get().getRawJson();
        }
        return eastMoneyStorageService.saveIndustryBase(eastMoneyApiClient.fetchIndustryBase()).getRawJson();
    }

    public String getIndustryKline(String industryCode) {
        Optional<IndustryKlineResponseEntity> existing = eastMoneyStorageService.findLatestIndustryKline(industryCode);
        if (isFresh(existing.map(IndustryKlineResponseEntity::getFetchedAt))) {
            return existing.get().getRawJson();
        }
        return eastMoneyStorageService.saveIndustryKline(industryCode, eastMoneyApiClient.fetchIndustryKline(industryCode)).getRawJson();
    }

    public String getStockReal(String stockCode) {
        Optional<StockRealResponseEntity> existing = eastMoneyStorageService.findLatestStockReal(stockCode);
        if (isFresh(existing.map(StockRealResponseEntity::getFetchedAt))) {
            return existing.get().getRawJson();
        }
        return eastMoneyStorageService.saveStockReal(stockCode, eastMoneyApiClient.fetchStockReal(stockCode)).getRawJson();
    }

    public String getStockKline(String stockCode) {
        Optional<StockKlineResponseEntity> existing = eastMoneyStorageService.findLatestStockKline(stockCode);
        if (isFresh(existing.map(StockKlineResponseEntity::getFetchedAt))) {
            return existing.get().getRawJson();
        }
        return eastMoneyStorageService.saveStockKline(stockCode, eastMoneyApiClient.fetchStockKline(stockCode)).getRawJson();
    }

    public String getStockPool(int pageNo) {
        Optional<StockPoolResponseEntity> existing = eastMoneyStorageService.findLatestStockPool(pageNo);
        if (isFresh(existing.map(StockPoolResponseEntity::getFetchedAt))) {
            return existing.get().getRawJson();
        }
        return eastMoneyStorageService.saveStockPool(pageNo, eastMoneyApiClient.fetchStockPool(pageNo)).getRawJson();
    }

    private boolean isFresh(Optional<LocalDateTime> fetchedAt) {
        return fetchedAt
            .map(value -> value.isAfter(LocalDateTime.now().minus(freshnessWindow)))
            .orElse(false);
    }
}
