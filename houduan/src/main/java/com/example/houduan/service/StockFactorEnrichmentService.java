package com.example.houduan.service;

import com.example.houduan.entity.StockKlineResponseEntity;
import com.example.houduan.entity.StockRealResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
public class StockFactorEnrichmentService {

    private final ObjectMapper objectMapper;
    private final EastMoneyStorageService eastMoneyStorageService;
    private final AShareMarketClockService marketClockService;
    private final Duration quoteFreshnessWindow;
    private final Duration klineFreshnessWindow;

    public StockFactorEnrichmentService(
        ObjectMapper objectMapper,
        EastMoneyStorageService eastMoneyStorageService,
        AShareMarketClockService marketClockService,
        @Value("${analysis.stage3.quote-freshness-minutes:25}") long quoteFreshnessMinutes,
        @Value("${analysis.stage3.kline-freshness-hours:96}") long klineFreshnessHours
    ) {
        this.objectMapper = objectMapper;
        this.eastMoneyStorageService = eastMoneyStorageService;
        this.marketClockService = marketClockService;
        this.quoteFreshnessWindow = Duration.ofMinutes(Math.max(5, quoteFreshnessMinutes));
        this.klineFreshnessWindow = Duration.ofHours(Math.max(24, klineFreshnessHours));
    }

    public Stage3Factor loadStage3Factor(String stockCode) {
        QuoteSignal quoteSignal = eastMoneyStorageService.findLatestStockReal(stockCode)
            .filter(entity -> isQuoteFresh(entity.getFetchedAt()))
            .map(this::parseQuoteSignal)
            .orElseGet(QuoteSignal::neutral);

        KlineSignal klineSignal = eastMoneyStorageService.findLatestStockKline(stockCode)
            .filter(entity -> isKlineFresh(entity.getFetchedAt()))
            .map(this::parseKlineSignal)
            .orElseGet(KlineSignal::neutral);

        LinkedHashSet<String> positiveSignals = new LinkedHashSet<>();
        positiveSignals.addAll(quoteSignal.positiveSignals());
        positiveSignals.addAll(klineSignal.positiveSignals());

        LinkedHashSet<String> riskTags = new LinkedHashSet<>();
        riskTags.addAll(quoteSignal.riskTags());
        riskTags.addAll(klineSignal.riskTags());

        return new Stage3Factor(
            quoteSignal.available(),
            klineSignal.available(),
            round1(quoteSignal.score()),
            round1(klineSignal.score()),
            round2(quoteSignal.priceVsAveragePct()),
            round2(quoteSignal.closeStrengthPct()),
            round2(quoteSignal.committeeRatioPct()),
            round2(klineSignal.breakoutPct()),
            round2(klineSignal.momentum5Pct()),
            round2(klineSignal.momentum10Pct()),
            round2(klineSignal.ma5DeviationPct()),
            round2(klineSignal.ma20DeviationPct()),
            round2(quoteSignal.riskPenalty() + klineSignal.riskPenalty()),
            quoteSignal.fetchedAt(),
            klineSignal.fetchedAt(),
            List.copyOf(positiveSignals),
            List.copyOf(riskTags)
        );
    }

    private boolean isQuoteFresh(LocalDateTime fetchedAt) {
        if (fetchedAt == null) {
            return false;
        }
        LocalDateTime now = marketClockService.now();
        if (marketClockService.isTradingSession(now)) {
            return !fetchedAt.plus(quoteFreshnessWindow).isBefore(now);
        }

        LocalDate referenceTradingDate = resolveReferenceTradingDate(now);
        return fetchedAt.toLocalDate().isEqual(referenceTradingDate);
    }

    private boolean isKlineFresh(LocalDateTime fetchedAt) {
        if (fetchedAt == null) {
            return false;
        }
        return !fetchedAt.plus(klineFreshnessWindow).isBefore(marketClockService.now());
    }

    private LocalDate resolveReferenceTradingDate(LocalDateTime now) {
        LocalDate currentDate = now.toLocalDate();
        if (marketClockService.isTradingDay(currentDate) && !now.toLocalTime().isBefore(LocalTime.of(15, 0))) {
            return currentDate;
        }
        return previousTradingWeekday(currentDate.minusDays(1));
    }

    private LocalDate previousTradingWeekday(LocalDate date) {
        LocalDate cursor = date;
        while (!marketClockService.isTradingDay(cursor)) {
            cursor = cursor.minusDays(1);
        }
        return cursor;
    }

    private QuoteSignal parseQuoteSignal(StockRealResponseEntity entity) {
        JsonNode dataNode = readPath(entity.getRawJson(), "data");
        if (dataNode == null || dataNode.isNull()) {
            return QuoteSignal.neutral();
        }

        double latestPrice = scaledPrice(dataNode.get("f43"));
        double openPrice = scaledPrice(dataNode.get("f46"));
        double highPrice = scaledPrice(dataNode.get("f44"));
        double lowPrice = scaledPrice(dataNode.get("f45"));
        double averagePrice = scaledPrice(dataNode.get("f71"));
        double committeeRatioPct = scaledPercent(dataNode.get("f191"));
        double outerVolume = numberValue(dataNode.get("f49"));
        double innerVolume = numberValue(dataNode.get("f161"));

        double priceVsAveragePct = percentChange(latestPrice, averagePrice);
        double openDrivePct = percentChange(latestPrice, openPrice);
        double closeStrengthPct = highPrice > lowPrice
            ? clamp((latestPrice - lowPrice) / (highPrice - lowPrice) * 100.0, 0, 100)
            : 50;
        double outerInnerRatio = innerVolume > 0 ? outerVolume / innerVolume : (outerVolume > 0 ? 2.0 : 1.0);
        double highPullbackPct = percentChange(latestPrice, highPrice);

        double quoteScore =
            100 * (
                0.30 * normalize(priceVsAveragePct, -1.2, 2.2) +
                0.25 * normalize(closeStrengthPct, 25, 90) +
                0.18 * normalize(openDrivePct, -1.5, 3.5) +
                0.15 * normalize(committeeRatioPct, -20, 35) +
                0.12 * normalize(outerInnerRatio, 0.85, 1.8)
            );

        List<String> positiveSignals = new ArrayList<>();
        List<String> riskTags = new ArrayList<>();
        double riskPenalty = 0;

        if (priceVsAveragePct >= 0.6) {
            positiveSignals.add("站上分时均价");
        }
        if (closeStrengthPct >= 70) {
            positiveSignals.add("日内收在高位");
        }
        if (committeeRatioPct >= 10) {
            positiveSignals.add("委比偏强");
        }
        if (outerInnerRatio >= 1.2) {
            positiveSignals.add("外盘强于内盘");
        }

        if (priceVsAveragePct <= -0.6) {
            riskTags.add("跌破分时均价");
            riskPenalty += 2.5;
        }
        if (closeStrengthPct <= 30) {
            riskTags.add("收在日内低位");
            riskPenalty += 2.5;
        }
        if (committeeRatioPct <= -10) {
            riskTags.add("委比偏弱");
            riskPenalty += 2.0;
        }
        if (highPullbackPct <= -2.5) {
            riskTags.add("盘中冲高回落");
            riskPenalty += 2.0;
        }

        return new QuoteSignal(
            true,
            clamp(quoteScore, 0, 100),
            priceVsAveragePct,
            closeStrengthPct,
            committeeRatioPct,
            riskPenalty,
            entity.getFetchedAt(),
            positiveSignals,
            riskTags
        );
    }

    private KlineSignal parseKlineSignal(StockKlineResponseEntity entity) {
        JsonNode klineNode = readPath(entity.getRawJson(), "data", "klines");
        if (klineNode == null || !klineNode.isArray() || klineNode.size() < 5) {
            return KlineSignal.neutral();
        }

        List<DailyBar> bars = new ArrayList<>();
        for (JsonNode item : klineNode) {
            String line = item.asText("");
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 11) {
                continue;
            }
            bars.add(new DailyBar(
                parts[0],
                parseDouble(parts[1]),
                parseDouble(parts[2]),
                parseDouble(parts[3]),
                parseDouble(parts[4]),
                parseDouble(parts[5]),
                parseDouble(parts[6]),
                parseDouble(parts[7]),
                parseDouble(parts[8]),
                parseDouble(parts[9]),
                parseDouble(parts[10])
            ));
        }

        if (bars.size() < 5) {
            return KlineSignal.neutral();
        }

        DailyBar latestBar = bars.get(bars.size() - 1);
        double latestClose = latestBar.closePrice();
        double ma5 = averageClose(bars, 5);
        double ma10 = averageClose(bars, 10);
        double ma20 = averageClose(bars, 20);
        double ma5DeviationPct = percentChange(latestClose, ma5);
        double ma20DeviationPct = percentChange(latestClose, ma20);
        double momentum5Pct = trailingChange(bars, 5);
        double momentum10Pct = trailingChange(bars, 10);
        double recent20High = trailingHigh(bars, 20);
        double breakoutPct = percentChange(latestClose, recent20High);
        double volumeExpansion = latestBar.volume() / Math.max(1, averageVolumeExcludingLatest(bars, 5));

        double bullishStructure =
            0.35 * boolScore(latestClose > ma5) +
            0.25 * boolScore(ma5 > ma10) +
            0.25 * boolScore(ma10 > ma20) +
            0.15 * boolScore(latestBar.closePrice() >= latestBar.openPrice());

        double technicalScore =
            100 * (
                0.30 * bullishStructure +
                0.22 * normalize(momentum5Pct, -4, 8) +
                0.18 * normalize(momentum10Pct, -6, 15) +
                0.16 * normalize(breakoutPct, -8, 3) +
                0.14 * normalize(volumeExpansion, 0.8, 2.2)
            );

        List<String> positiveSignals = new ArrayList<>();
        List<String> riskTags = new ArrayList<>();
        double riskPenalty = 0;

        if (latestClose > ma20 && ma5 > ma10 && ma10 > ma20) {
            positiveSignals.add("均线多头排列");
        }
        if (breakoutPct >= -1) {
            positiveSignals.add("接近20日新高");
        }
        if (momentum5Pct >= 3) {
            positiveSignals.add("5日动量较强");
        }
        if (volumeExpansion >= 1.2) {
            positiveSignals.add("日线放量");
        }

        if (latestClose < ma10) {
            riskTags.add("跌破10日线");
            riskPenalty += 2.5;
        }
        if (latestClose < ma20) {
            riskTags.add("跌破20日线");
            riskPenalty += 3.0;
        }
        if (breakoutPct <= -8) {
            riskTags.add("距离20日高点较远");
            riskPenalty += 2.0;
        }
        if (momentum5Pct <= -4) {
            riskTags.add("短线动量转弱");
            riskPenalty += 2.0;
        }
        if (volumeExpansion < 0.8 && momentum5Pct < 0) {
            riskTags.add("缩量走弱");
            riskPenalty += 1.5;
        }

        return new KlineSignal(
            true,
            clamp(technicalScore, 0, 100),
            breakoutPct,
            momentum5Pct,
            momentum10Pct,
            ma5DeviationPct,
            ma20DeviationPct,
            riskPenalty,
            entity.getFetchedAt(),
            positiveSignals,
            riskTags
        );
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

    private double numberValue(JsonNode node) {
        return node == null || node.isNull() ? 0 : node.asDouble(0);
    }

    private double scaledPrice(JsonNode node) {
        return numberValue(node) / 100.0;
    }

    private double scaledPercent(JsonNode node) {
        return numberValue(node) / 100.0;
    }

    private double percentChange(double current, double base) {
        if (current <= 0 || base <= 0) {
            return 0;
        }
        return (current - base) / base * 100.0;
    }

    private double normalize(double value, double min, double max) {
        if (max <= min) {
            return 0;
        }
        return clamp((value - min) / (max - min), 0, 1);
    }

    private double boolScore(boolean value) {
        return value ? 1.0 : 0.0;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double averageClose(List<DailyBar> bars, int window) {
        return averageDouble(trailingSlice(bars, window).stream().map(DailyBar::closePrice).toList());
    }

    private double averageVolumeExcludingLatest(List<DailyBar> bars, int window) {
        if (bars.size() <= 1) {
            return 0;
        }
        int fromIndex = Math.max(0, bars.size() - 1 - window);
        int toIndex = bars.size() - 1;
        if (fromIndex >= toIndex) {
            return 0;
        }
        return averageDouble(bars.subList(fromIndex, toIndex).stream().map(DailyBar::volume).toList());
    }

    private double trailingChange(List<DailyBar> bars, int days) {
        if (bars.size() <= days) {
            return 0;
        }
        double startClose = bars.get(bars.size() - 1 - days).closePrice();
        double latestClose = bars.get(bars.size() - 1).closePrice();
        return percentChange(latestClose, startClose);
    }

    private double trailingHigh(List<DailyBar> bars, int window) {
        return trailingSlice(bars, window).stream()
            .mapToDouble(DailyBar::highPrice)
            .max()
            .orElse(0);
    }

    private List<DailyBar> trailingSlice(List<DailyBar> bars, int window) {
        int fromIndex = Math.max(0, bars.size() - window);
        return bars.subList(fromIndex, bars.size());
    }

    private double averageDouble(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (Double value : values) {
            total += value == null ? 0 : value;
        }
        return total / values.size();
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record Stage3Factor(
        boolean quoteAvailable,
        boolean technicalAvailable,
        double quoteScore,
        double technicalScore,
        double priceVsAveragePct,
        double closeStrengthPct,
        double committeeRatioPct,
        double breakoutPct,
        double momentum5Pct,
        double momentum10Pct,
        double ma5DeviationPct,
        double ma20DeviationPct,
        double riskPenalty,
        LocalDateTime quoteFetchedAt,
        LocalDateTime klineFetchedAt,
        List<String> positiveSignals,
        List<String> riskTags
    ) {
    }

    private record QuoteSignal(
        boolean available,
        double score,
        double priceVsAveragePct,
        double closeStrengthPct,
        double committeeRatioPct,
        double riskPenalty,
        LocalDateTime fetchedAt,
        List<String> positiveSignals,
        List<String> riskTags
    ) {

        static QuoteSignal neutral() {
            return new QuoteSignal(false, 50, 0, 50, 0, 0, null, List.of(), List.of());
        }
    }

    private record KlineSignal(
        boolean available,
        double score,
        double breakoutPct,
        double momentum5Pct,
        double momentum10Pct,
        double ma5DeviationPct,
        double ma20DeviationPct,
        double riskPenalty,
        LocalDateTime fetchedAt,
        List<String> positiveSignals,
        List<String> riskTags
    ) {

        static KlineSignal neutral() {
            return new KlineSignal(false, 50, 0, 0, 0, 0, 0, 0, null, List.of(), List.of());
        }
    }

    private record DailyBar(
        String tradeDate,
        double openPrice,
        double closePrice,
        double highPrice,
        double lowPrice,
        double volume,
        double amount,
        double amplitudePct,
        double changePct,
        double changeAmount,
        double turnoverPct
    ) {
    }
}
