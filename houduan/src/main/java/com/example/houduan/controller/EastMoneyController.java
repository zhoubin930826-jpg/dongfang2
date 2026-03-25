package com.example.houduan.controller;

import com.example.houduan.service.EastMoneyQueryService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class EastMoneyController {

    private final EastMoneyQueryService eastMoneyQueryService;

    public EastMoneyController(EastMoneyQueryService eastMoneyQueryService) {
        this.eastMoneyQueryService = eastMoneyQueryService;
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
    public String getStockRealData(@RequestParam String stockCode) {
        return eastMoneyQueryService.getStockReal(stockCode);
    }

    @GetMapping("/api/stock/kline")
    public String getStockKlineData(@RequestParam String stockCode) {
        return eastMoneyQueryService.getStockKline(stockCode);
    }

    @GetMapping("/api/stock/pool")
    public String getStockPoolData(@RequestParam(defaultValue = "1") Integer pn) {
        return eastMoneyQueryService.getStockPool(pn);
    }
}
