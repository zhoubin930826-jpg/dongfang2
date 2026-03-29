package com.example.houduan.controller;

import com.example.houduan.service.EastMoneyQueryService;
import com.example.houduan.service.StockIntradayAnalysisService;
import com.example.houduan.service.StockOpportunityAnalysisService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class EastMoneyController {

    private final EastMoneyQueryService eastMoneyQueryService;
    private final StockOpportunityAnalysisService stockOpportunityAnalysisService;
    private final StockIntradayAnalysisService stockIntradayAnalysisService;

    public EastMoneyController(
        EastMoneyQueryService eastMoneyQueryService,
        StockOpportunityAnalysisService stockOpportunityAnalysisService,
        StockIntradayAnalysisService stockIntradayAnalysisService
    ) {
        this.eastMoneyQueryService = eastMoneyQueryService;
        this.stockOpportunityAnalysisService = stockOpportunityAnalysisService;
        this.stockIntradayAnalysisService = stockIntradayAnalysisService;
    }

    @GetMapping("/api/industry/base")
    public String getIndustryBaseData() {
        return eastMoneyQueryService.getIndustryBase();
    }

    @GetMapping("/api/industry/kline")
    public String getIndustryKlineData(@RequestParam String industryCode) {
        return eastMoneyQueryService.getIndustryKline(industryCode);
    }

    @GetMapping("/api/stock/real")
    public String getStockRealData(@RequestParam String stockCode, @RequestParam(required = false) Integer market) {
        return eastMoneyQueryService.getStockReal(stockCode, market);
    }

    @GetMapping("/api/stock/kline")
    public String getStockKlineData(@RequestParam String stockCode, @RequestParam(required = false) Integer market) {
        return eastMoneyQueryService.getStockKline(stockCode, market);
    }

    @GetMapping("/api/stock/pool")
    public String getStockPoolData(@RequestParam(defaultValue = "1") Integer pn) {
        return eastMoneyQueryService.getStockPool(pn);
    }

    @GetMapping("/api/analysis/opportunities")
    public Map<String, Object> getStockOpportunityAnalysis(@RequestParam(defaultValue = "30") Integer limit) {
        return stockOpportunityAnalysisService.buildOpportunityAnalysis(limit);
    }

    @GetMapping("/api/analysis/intraday")
    public Map<String, Object> getStockIntradayAnalysis(
        @RequestParam String stockCode,
        @RequestParam(required = false) Integer market
    ) {
        return stockIntradayAnalysisService.buildIntradayAnalysis(stockCode, market);
    }
}
