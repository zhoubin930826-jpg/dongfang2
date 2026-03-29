package com.example.houduan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StockIntradayAnalysisService {

    private final ObjectMapper objectMapper;
    private final EastMoneyApiClient eastMoneyApiClient;
    private final Duration cacheTtl;
    private final Map<String, CachedIntradayAnalysis> cache = new ConcurrentHashMap<>();

    public StockIntradayAnalysisService(
        ObjectMapper objectMapper,
        EastMoneyApiClient eastMoneyApiClient,
        @Value("${analysis.intraday.cache-ms:120000}") long cacheMs
    ) {
        this.objectMapper = objectMapper;
        this.eastMoneyApiClient = eastMoneyApiClient;
        this.cacheTtl = Duration.ofMillis(Math.max(30000, cacheMs));
    }

    public Map<String, Object> buildIntradayAnalysis(String stockCode, Integer market) {
        String cacheKey = (market == null ? "auto" : market) + ":" + stockCode;
        CachedIntradayAnalysis cachedAnalysis = cache.get(cacheKey);
        if (cachedAnalysis != null && cachedAnalysis.expiresAt().isAfter(LocalDateTime.now())) {
            return cachedAnalysis.payload();
        }

        Map<String, Object> payload = loadIntradayAnalysis(stockCode, market);
        cache.put(cacheKey, new CachedIntradayAnalysis(payload, LocalDateTime.now().plus(cacheTtl)));
        return payload;
    }

    private Map<String, Object> loadIntradayAnalysis(String stockCode, Integer market) {
        JsonNode dataNode = readPath(eastMoneyApiClient.fetchStockTrends(stockCode, market, 1), "data");
        if (dataNode == null || dataNode.isNull()) {
            throw new IllegalStateException("未获取到分时数据");
        }

        List<MinuteBar> minuteBars = parseMinuteBars(dataNode.get("trends"));
        if (minuteBars.isEmpty()) {
            throw new IllegalStateException("分时数据为空");
        }

        MinuteBar firstBar = minuteBars.get(0);
        MinuteBar latestBar = minuteBars.get(minuteBars.size() - 1);
        double preClose = numberValue(dataNode.get("preClose"));
        double latestPrice = latestBar.closePrice();
        double openPrice = firstBar.openPrice();
        double highPrice = minuteBars.stream().mapToDouble(MinuteBar::highPrice).max().orElse(latestPrice);
        double lowPrice = minuteBars.stream().mapToDouble(MinuteBar::lowPrice).min().orElse(latestPrice);
        double latestAveragePrice = latestBar.averagePrice();
        double dayChangePct = percentChange(latestPrice, preClose);
        double fromOpenPct = percentChange(latestPrice, openPrice);
        double amplitudePct = percentRange(highPrice, lowPrice, preClose);
        double highPullbackPct = percentChange(latestPrice, highPrice);
        double vwapPremiumPct = percentChange(latestPrice, latestAveragePrice);
        double closeStrengthPct = highPrice > lowPrice
            ? clamp((latestPrice - lowPrice) / (highPrice - lowPrice) * 100.0, 0, 100)
            : 50;
        double last30MinChangePct = computeTrailingChange(minuteBars, 30);
        double last60MinChangePct = computeTrailingChange(minuteBars, 60);
        double last30MinVolumeRatio = computeRecentVolumeRatio(minuteBars, 30);
        double afternoonChangePct = computeSessionChange(minuteBars, "13:01");

        List<String> positiveSignals = new ArrayList<>();
        List<String> riskTags = new ArrayList<>();

        if (dayChangePct >= 3) {
            positiveSignals.add("日内涨幅较强");
        }
        if (fromOpenPct >= 1.5) {
            positiveSignals.add("开盘后持续抬升");
        }
        if (vwapPremiumPct >= 0.6) {
            positiveSignals.add("价格站上分时均价");
        }
        if (closeStrengthPct >= 70) {
            positiveSignals.add("价格靠近日内高位");
        }
        if (last30MinChangePct >= 0.8) {
            positiveSignals.add("尾盘资金继续推升");
        }
        if (last30MinVolumeRatio >= 1.5) {
            positiveSignals.add("近30分钟放量");
        }
        if (afternoonChangePct >= 1) {
            positiveSignals.add("午后走势偏强");
        }

        if (vwapPremiumPct <= -0.6) {
            riskTags.add("跌破分时均价");
        }
        if (last30MinChangePct <= -0.8) {
            riskTags.add("尾盘走弱");
        }
        if (highPullbackPct <= -2.5) {
            riskTags.add("冲高回落");
        }
        if (closeStrengthPct <= 30) {
            riskTags.add("价格靠近日内低位");
        }
        if (amplitudePct >= 8) {
            riskTags.add("日内振幅较大");
        }
        if (last30MinVolumeRatio >= 2.2 && dayChangePct < 1) {
            riskTags.add("放量但价格承接一般");
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("stockCode", stockCode);
        summary.put("market", market);
        summary.put("name", textValue(dataNode.get("name")));
        summary.put("date", latestBar.timestamp().length() >= 10 ? latestBar.timestamp().substring(0, 10) : latestBar.timestamp());
        summary.put("fetchedAt", LocalDateTime.now().toString());
        summary.put("preClose", round2(preClose));
        summary.put("openPrice", round2(openPrice));
        summary.put("latestPrice", round2(latestPrice));
        summary.put("highPrice", round2(highPrice));
        summary.put("lowPrice", round2(lowPrice));
        summary.put("averagePrice", round2(latestAveragePrice));
        summary.put("dayChangePct", round2(dayChangePct));
        summary.put("fromOpenPct", round2(fromOpenPct));
        summary.put("amplitudePct", round2(amplitudePct));
        summary.put("highPullbackPct", round2(highPullbackPct));
        summary.put("vwapPremiumPct", round2(vwapPremiumPct));
        summary.put("closeStrengthPct", round2(closeStrengthPct));
        summary.put("last30MinChangePct", round2(last30MinChangePct));
        summary.put("last60MinChangePct", round2(last60MinChangePct));
        summary.put("last30MinVolumeRatio", round2(last30MinVolumeRatio));
        summary.put("afternoonChangePct", round2(afternoonChangePct));
        summary.put("sessionTone", resolveSessionTone(dayChangePct, vwapPremiumPct, last30MinChangePct, closeStrengthPct));

        List<Map<String, Object>> points = new ArrayList<>(minuteBars.size());
        for (MinuteBar minuteBar : minuteBars) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("time", minuteBar.timestamp().length() >= 16 ? minuteBar.timestamp().substring(11) : minuteBar.timestamp());
            point.put("closePrice", round2(minuteBar.closePrice()));
            point.put("averagePrice", round2(minuteBar.averagePrice()));
            point.put("volume", round0(minuteBar.volume()));
            point.put("amount", round0(minuteBar.amount()));
            points.add(point);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", summary);
        payload.put("positiveSignals", positiveSignals);
        payload.put("riskTags", riskTags);
        payload.put("points", points);
        return payload;
    }

    private List<MinuteBar> parseMinuteBars(JsonNode trendsNode) {
        if (trendsNode == null || !trendsNode.isArray()) {
            return List.of();
        }

        List<MinuteBar> minuteBars = new ArrayList<>();
        for (JsonNode item : trendsNode) {
            String rawLine = item.asText("");
            if (rawLine.isBlank()) {
                continue;
            }
            String[] parts = rawLine.split(",");
            if (parts.length < 8) {
                continue;
            }
            minuteBars.add(new MinuteBar(
                parts[0],
                parseDouble(parts[1]),
                parseDouble(parts[2]),
                parseDouble(parts[3]),
                parseDouble(parts[4]),
                parseDouble(parts[5]),
                parseDouble(parts[6]),
                parseDouble(parts[7])
            ));
        }
        return minuteBars;
    }

    private double computeTrailingChange(List<MinuteBar> minuteBars, int interval) {
        if (minuteBars.isEmpty()) {
            return 0;
        }
        int startIndex = Math.max(0, minuteBars.size() - 1 - interval);
        double startPrice = minuteBars.get(startIndex).closePrice();
        double latestPrice = minuteBars.get(minuteBars.size() - 1).closePrice();
        return percentChange(latestPrice, startPrice);
    }

    private double computeRecentVolumeRatio(List<MinuteBar> minuteBars, int interval) {
        if (minuteBars.isEmpty()) {
            return 0;
        }
        int slice = Math.min(interval, minuteBars.size());
        double recentVolume = minuteBars.subList(minuteBars.size() - slice, minuteBars.size())
            .stream()
            .mapToDouble(MinuteBar::volume)
            .sum();
        double baselineAverage = minuteBars.stream().mapToDouble(MinuteBar::volume).average().orElse(0);
        if (baselineAverage <= 0) {
            return 0;
        }
        return recentVolume / (baselineAverage * slice);
    }

    private double computeSessionChange(List<MinuteBar> minuteBars, String startTime) {
        if (minuteBars.isEmpty()) {
            return 0;
        }
        MinuteBar sessionStart = minuteBars.stream()
            .filter(bar -> bar.timestamp().length() >= 16 && bar.timestamp().substring(11).compareTo(startTime) >= 0)
            .findFirst()
            .orElse(minuteBars.get(0));
        return percentChange(minuteBars.get(minuteBars.size() - 1).closePrice(), sessionStart.closePrice());
    }

    private String resolveSessionTone(double dayChangePct, double vwapPremiumPct, double last30MinChangePct, double closeStrengthPct) {
        if (dayChangePct >= 3 && vwapPremiumPct >= 0.5 && closeStrengthPct >= 70) {
            return "分时强势";
        }
        if (last30MinChangePct <= -0.8 || vwapPremiumPct <= -0.6) {
            return "分时转弱";
        }
        if (Math.abs(dayChangePct) <= 1 && Math.abs(last30MinChangePct) <= 0.5) {
            return "震荡";
        }
        return "中性偏强";
    }

    private JsonNode readPath(String rawJson, String... path) {
        try {
            JsonNode current = objectMapper.readTree(rawJson);
            for (String part : path) {
                if (current == null) {
                    return null;
                }
                current = current.get(part);
            }
            return current;
        } catch (IOException e) {
            return null;
        }
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    private double numberValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        return node.asDouble(0);
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private double percentChange(double current, double base) {
        if (base == 0) {
            return 0;
        }
        return (current - base) / base * 100.0;
    }

    private double percentRange(double high, double low, double base) {
        if (base == 0) {
            return 0;
        }
        return (high - low) / base * 100.0;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round0(double value) {
        return Math.round(value);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record CachedIntradayAnalysis(Map<String, Object> payload, LocalDateTime expiresAt) {
    }

    private record MinuteBar(
        String timestamp,
        double openPrice,
        double closePrice,
        double highPrice,
        double lowPrice,
        double volume,
        double amount,
        double averagePrice
    ) {
    }
}
