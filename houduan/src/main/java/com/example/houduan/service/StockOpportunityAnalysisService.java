package com.example.houduan.service;

import com.example.houduan.entity.IndustryBaseResponseEntity;
import com.example.houduan.entity.StockPoolResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class StockOpportunityAnalysisService {

    private static final List<IndexDefinition> CORE_INDICES = List.of(
        new IndexDefinition("上证指数", "000001", "1.000001"),
        new IndexDefinition("深证成指", "399001", "0.399001"),
        new IndexDefinition("创业板指", "399006", "0.399006"),
        new IndexDefinition("沪深300", "000300", "1.000300")
    );

    private final ObjectMapper objectMapper;
    private final EastMoneyApiClient eastMoneyApiClient;
    private final EastMoneyStorageService eastMoneyStorageService;
    private final Duration indexCacheTtl;

    private final Object indexCacheMonitor = new Object();
    private CachedIndexBundle cachedIndexBundle;

    public StockOpportunityAnalysisService(
        ObjectMapper objectMapper,
        EastMoneyApiClient eastMoneyApiClient,
        EastMoneyStorageService eastMoneyStorageService,
        @Value("${analysis.market-index.cache-ms:120000}") long indexCacheMs
    ) {
        this.objectMapper = objectMapper;
        this.eastMoneyApiClient = eastMoneyApiClient;
        this.eastMoneyStorageService = eastMoneyStorageService;
        this.indexCacheTtl = Duration.ofMillis(Math.max(30000, indexCacheMs));
    }

    public Map<String, Object> buildOpportunityAnalysis(int limit) {
        int safeLimit = Math.min(Math.max(limit, 10), 100);
        StockPoolSnapshot stockPoolSnapshot = loadStockPoolSnapshot();
        IndustrySnapshot industrySnapshot = loadIndustrySnapshot();
        MarketIndexBundle marketIndexBundle = getMarketIndexBundle();
        MarketBreadth breadth = buildMarketBreadth(stockPoolSnapshot.rows(), industrySnapshot.rows(), marketIndexBundle.indices());
        List<IndustryRow> topIndustries = resolveTopIndustries(industrySnapshot.rows(), 8);
        List<String> warnings = buildWarnings(stockPoolSnapshot, industrySnapshot, marketIndexBundle);

        Map<String, IndustryRow> industryByName = new LinkedHashMap<>();
        for (IndustryRow row : industrySnapshot.rows()) {
            if (row.industryName() != null && !row.industryName().isBlank()) {
                industryByName.putIfAbsent(row.industryName(), row);
            }
        }

        List<StockOpportunity> opportunities = new ArrayList<>();
        for (StockRow stockRow : stockPoolSnapshot.rows()) {
            opportunities.add(scoreStock(stockRow, industryByName.get(stockRow.industryName()), breadth.sentimentScore()));
        }

        opportunities.sort(Comparator.comparingDouble(StockOpportunity::score).reversed());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("analysisGeneratedAt", LocalDateTime.now().toString());
        summary.put("marketSentimentScore", round1(breadth.sentimentScore()));
        summary.put("marketTone", resolveMarketTone(breadth.sentimentScore()));
        summary.put("stockPoolReferenceFetchedAt", toText(stockPoolSnapshot.referenceFetchedAt()));
        summary.put("stockPoolOldestFetchedAt", toText(stockPoolSnapshot.oldestFetchedAt()));
        summary.put("stockPoolLatestFetchedAt", toText(stockPoolSnapshot.latestFetchedAt()));
        summary.put("industryBaseFetchedAt", toText(industrySnapshot.fetchedAt()));
        summary.put("marketIndexFetchedAt", toText(marketIndexBundle.fetchedAt()));
        summary.put("totalStocks", stockPoolSnapshot.rows().size());
        summary.put("candidateCount", Math.min(opportunities.size(), safeLimit));
        summary.put("pageCount", stockPoolSnapshot.pageCount());
        summary.put("missingPageCount", stockPoolSnapshot.missingPageCount());
        summary.put("advanceCount", breadth.advanceCount());
        summary.put("declineCount", breadth.declineCount());
        summary.put("flatCount", breadth.flatCount());
        summary.put("limitUpCount", breadth.limitUpCount());
        summary.put("limitDownCount", breadth.limitDownCount());
        summary.put("averageChangePct", round2(breadth.averageChangePct()));
        summary.put("advanceRatio", round2(breadth.advanceRatio() * 100));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", summary);
        payload.put("indices", marketIndexBundle.indices().stream().map(this::toIndexPayload).toList());
        payload.put("topIndustries", topIndustries.stream().map(this::toIndustryPayload).toList());
        payload.put("candidates", opportunities.stream().limit(safeLimit).map(this::toCandidatePayload).toList());
        payload.put("warnings", warnings);
        return payload;
    }

    private StockPoolSnapshot loadStockPoolSnapshot() {
        StockPoolResponseEntity referencePage = eastMoneyStorageService.findLatestStockPool(1)
            .orElseThrow(() -> new IllegalStateException("本地股票池为空，请先等待 stock-pool 定时任务完成"));

        int pageCount = eastMoneyStorageService.extractTotalPages(
            referencePage.getRawJson(),
            eastMoneyApiClient.getStockPoolPageSize()
        );
        if (pageCount <= 0) {
            pageCount = 1;
        }

        LocalDateTime referenceFetchedAt = referencePage.getFetchedAt();
        LocalDateTime oldestFetchedAt = referenceFetchedAt;
        LocalDateTime latestFetchedAt = referenceFetchedAt;
        int missingPageCount = 0;

        Map<String, StockRow> rowsByCode = new LinkedHashMap<>();
        for (int pageNo = 1; pageNo <= pageCount; pageNo++) {
            Optional<StockPoolResponseEntity> pageEntity = eastMoneyStorageService.findLatestStockPoolAtOrBefore(pageNo, referenceFetchedAt);
            if (pageEntity.isEmpty()) {
                missingPageCount++;
                continue;
            }
            StockPoolResponseEntity entity = pageEntity.get();
            oldestFetchedAt = min(oldestFetchedAt, entity.getFetchedAt());
            latestFetchedAt = max(latestFetchedAt, entity.getFetchedAt());
            for (StockRow stockRow : parseStockRows(entity.getRawJson())) {
                rowsByCode.putIfAbsent(stockRow.stockCode(), stockRow);
            }
        }

        return new StockPoolSnapshot(
            new ArrayList<>(rowsByCode.values()),
            referenceFetchedAt,
            oldestFetchedAt,
            latestFetchedAt,
            pageCount,
            missingPageCount
        );
    }

    private IndustrySnapshot loadIndustrySnapshot() {
        Optional<IndustryBaseResponseEntity> latestIndustryBase = eastMoneyStorageService.findLatestIndustryBase();
        if (latestIndustryBase.isEmpty()) {
            return new IndustrySnapshot(Collections.emptyList(), null);
        }
        IndustryBaseResponseEntity entity = latestIndustryBase.get();
        return new IndustrySnapshot(parseIndustryRows(entity.getRawJson()), entity.getFetchedAt());
    }

    private MarketIndexBundle getMarketIndexBundle() {
        CachedIndexBundle localCache = cachedIndexBundle;
        if (localCache != null && localCache.expiresAt().isAfter(LocalDateTime.now())) {
            return localCache.bundle();
        }

        synchronized (indexCacheMonitor) {
            CachedIndexBundle refreshCheck = cachedIndexBundle;
            if (refreshCheck != null && refreshCheck.expiresAt().isAfter(LocalDateTime.now())) {
                return refreshCheck.bundle();
            }

            MarketIndexBundle freshBundle = loadFreshMarketIndices();
            if (!freshBundle.indices().isEmpty()) {
                cachedIndexBundle = new CachedIndexBundle(freshBundle, LocalDateTime.now().plus(indexCacheTtl));
                return freshBundle;
            }
            if (refreshCheck != null) {
                return refreshCheck.bundle();
            }
            return freshBundle;
        }
    }

    private MarketIndexBundle loadFreshMarketIndices() {
        List<MarketIndex> indices = new ArrayList<>();
        LocalDateTime fetchedAt = LocalDateTime.now();

        for (IndexDefinition definition : CORE_INDICES) {
            try {
                JsonNode dataNode = readPath(eastMoneyApiClient.fetchMarketIndex(definition.secId()), "data");
                if (dataNode == null || dataNode.isNull()) {
                    continue;
                }
                indices.add(new MarketIndex(
                    definition.label(),
                    definition.code(),
                    definition.secId(),
                    scaledPrice(dataNode.get("f43")),
                    scaledPercent(dataNode.get("f170")),
                    scaledPrice(dataNode.get("f169")),
                    scaledPrice(dataNode.get("f46")),
                    scaledPrice(dataNode.get("f44")),
                    scaledPrice(dataNode.get("f45")),
                    scaledPrice(dataNode.get("f60")),
                    numberValue(dataNode.get("f47")),
                    numberValue(dataNode.get("f48"))
                ));
            } catch (Exception ignored) {
                // Ignore single-index failure and keep the analysis page available.
            }
        }

        return new MarketIndexBundle(indices, fetchedAt);
    }

    private List<StockRow> parseStockRows(String rawJson) {
        JsonNode diffNode = readPath(rawJson, "data", "diff");
        if (diffNode == null) {
            return Collections.emptyList();
        }

        List<StockRow> rows = new ArrayList<>();
        if (diffNode.isArray()) {
            diffNode.elements().forEachRemaining(item -> addStockRow(rows, item));
        } else if (diffNode.isObject()) {
            diffNode.elements().forEachRemaining(item -> addStockRow(rows, item));
        }
        return rows;
    }

    private void addStockRow(List<StockRow> rows, JsonNode item) {
        String stockCode = textValue(item.get("f12"));
        if (stockCode.isBlank()) {
            return;
        }

        rows.add(new StockRow(
            stockCode,
            intValue(item.get("f13")),
            textValue(item.get("f14")),
            scaledPrice(item.get("f2")),
            scaledPercent(item.get("f3")),
            scaledPrice(item.get("f4")),
            numberValue(item.get("f6")),
            scaledPercent(item.get("f7")),
            scaledPercent(item.get("f8")),
            numberValue(item.get("f10")),
            numberValue(item.get("f62")),
            scaledPercent(item.get("f24")),
            scaledPercent(item.get("f25")),
            numberValue(item.get("f20")),
            numberValue(item.get("f21")),
            numberValue(item.get("f23")),
            numberValue(item.get("f115")),
            textValue(item.get("f100")),
            textValue(item.get("f102")),
            textValue(item.get("f103"))
        ));
    }

    private List<IndustryRow> parseIndustryRows(String rawJson) {
        JsonNode diffNode = readPath(rawJson, "data", "diff");
        if (diffNode == null || !diffNode.isArray()) {
            return Collections.emptyList();
        }

        List<IndustryRow> rows = new ArrayList<>();
        for (JsonNode item : diffNode) {
            String industryCode = textValue(item.get("f12"));
            if (industryCode.isBlank()) {
                continue;
            }
            rows.add(new IndustryRow(
                industryCode,
                textValue(item.get("f14")),
                numberValue(item.get("f3")),
                numberValue(item.get("f62")),
                intValue(item.get("f104")),
                intValue(item.get("f105")),
                intValue(item.get("f106")),
                textValue(item.get("f128")),
                textValue(item.get("f140"))
            ));
        }
        return rows;
    }

    private MarketBreadth buildMarketBreadth(
        List<StockRow> stockRows,
        List<IndustryRow> industryRows,
        List<MarketIndex> marketIndices
    ) {
        if (stockRows.isEmpty()) {
            return new MarketBreadth(0, 0, 0, 0, 0, 0, 0, 0);
        }

        int advanceCount = 0;
        int declineCount = 0;
        int flatCount = 0;
        int limitUpCount = 0;
        int limitDownCount = 0;
        double totalChangePct = 0;

        for (StockRow row : stockRows) {
            totalChangePct += row.changePct();
            if (row.changePct() > 0) {
                advanceCount++;
            } else if (row.changePct() < 0) {
                declineCount++;
            } else {
                flatCount++;
            }
            if (row.changePct() >= 9.8) {
                limitUpCount++;
            }
            if (row.changePct() <= -9.8) {
                limitDownCount++;
            }
        }

        double averageChangePct = totalChangePct / stockRows.size();
        double advanceRatio = (double) advanceCount / stockRows.size();
        double averageIndexChangePct = average(marketIndices.stream().map(MarketIndex::changePct).toList());
        double averageIndustryChangePct = average(resolveTopIndustries(industryRows, 5).stream().map(IndustryRow::changePct).toList());

        double breadthScore =
            100 * (
                0.5 * normalize(advanceRatio, 0.35, 0.7) +
                0.3 * normalize(averageChangePct, -1.5, 2.5) +
                0.2 * normalize(limitUpCount - limitDownCount, -20, 120)
            );
        double indexScore = 100 * normalize(averageIndexChangePct, -1.0, 2.2);
        double industryScore = 100 * normalize(averageIndustryChangePct, -0.5, 4.5);
        double sentimentScore = 0.45 * breadthScore + 0.35 * indexScore + 0.20 * industryScore;

        return new MarketBreadth(
            advanceCount,
            declineCount,
            flatCount,
            limitUpCount,
            limitDownCount,
            averageChangePct,
            clamp(sentimentScore, 0, 100),
            advanceRatio
        );
    }

    private List<IndustryRow> resolveTopIndustries(List<IndustryRow> industryRows, int limit) {
        List<IndustryRow> rows = new ArrayList<>(industryRows);
        rows.sort(Comparator
            .comparingDouble(IndustryRow::changePct).reversed()
            .thenComparing(Comparator.comparingDouble(IndustryRow::netInflow).reversed()));
        return rows.size() <= limit ? rows : rows.subList(0, limit);
    }

    private StockOpportunity scoreStock(StockRow stockRow, IndustryRow industryRow, double marketSentimentScore) {
        double trendScore =
            100 * (
                0.45 * normalize(stockRow.changePct(), -2, 9) +
                0.30 * normalize(stockRow.sixtyDayPct(), -15, 40) +
                0.15 * normalize(stockRow.yearToDatePct(), -20, 60) +
                0.10 * normalize(stockRow.amplitudePct(), 1, 10)
            );
        double capitalScore =
            100 * (
                0.50 * normalize(stockRow.netInflow(), 0, 200_000_000d) +
                0.25 * normalize(stockRow.volumeRatio(), 0.8, 3.5) +
                0.25 * normalize(stockRow.turnoverRatePct(), 1, 10)
            );
        double liquidityScore =
            100 * (
                0.75 * normalize(stockRow.dealAmount(), 80_000_000d, 5_000_000_000d) +
                0.25 * normalize(stockRow.marketCap(), 3_000_000_000d, 150_000_000_000d)
            );
        double qualityScore =
            100 * (
                0.45 * scorePreferredRange(stockRow.peTtm(), 8, 45, 120) +
                0.20 * scorePreferredRange(stockRow.pb(), 1, 6, 15) +
                0.35 * normalize(stockRow.marketCap(), 5_000_000_000d, 150_000_000_000d)
            );

        double sectorScore = 40;
        double sectorChangePct = 0;
        double sectorBreadth = 0.5;
        double sectorNetInflow = 0;
        if (industryRow != null) {
            sectorChangePct = industryRow.changePct();
            sectorNetInflow = industryRow.netInflow();
            int total = industryRow.advanceCount() + industryRow.declineCount() + industryRow.flatCount();
            sectorBreadth = total > 0 ? (double) industryRow.advanceCount() / total : 0.5;
            sectorScore =
                100 * (
                    0.5 * normalize(sectorChangePct, -1, 4) +
                    0.3 * normalize(sectorNetInflow, 0, 500_000_000d) +
                    0.2 * normalize(sectorBreadth, 0.35, 0.75)
                );
        }

        List<String> riskTags = buildRiskTags(stockRow, industryRow);
        double riskPenalty = 0;
        if (containsRiskFlag(stockRow.stockName())) {
            riskPenalty += 35;
        }
        if (stockRow.peTtm() <= 0) {
            riskPenalty += 6;
        }
        if (stockRow.pb() > 15) {
            riskPenalty += 4;
        }
        if (stockRow.marketCap() > 0 && stockRow.marketCap() < 3_000_000_000d) {
            riskPenalty += 6;
        }
        if (stockRow.amplitudePct() > 12) {
            riskPenalty += 6;
        }
        if (stockRow.turnoverRatePct() > 25) {
            riskPenalty += 5;
        }
        if (industryRow == null) {
            riskPenalty += 4;
        }

        double baseScore =
            0.28 * trendScore +
            0.27 * capitalScore +
            0.19 * sectorScore +
            0.14 * liquidityScore +
            0.12 * qualityScore;
        double marketBoost = (marketSentimentScore - 50) * 0.08;
        double finalScore = clamp(baseScore + marketBoost - riskPenalty, 0, 100);

        List<String> reasons = new ArrayList<>();
        if (stockRow.changePct() >= 3) {
            reasons.add("当日涨幅较强");
        }
        if (stockRow.netInflow() >= 100_000_000d) {
            reasons.add("主力净流入明显");
        }
        if (stockRow.volumeRatio() >= 1.5) {
            reasons.add("量比放大");
        }
        if (stockRow.turnoverRatePct() >= 2 && stockRow.turnoverRatePct() <= 12) {
            reasons.add("换手活跃");
        }
        if (industryRow != null && industryRow.changePct() >= 1.5) {
            reasons.add("所在行业走强");
        }
        if (industryRow != null && industryRow.netInflow() >= 200_000_000d) {
            reasons.add("行业资金流入靠前");
        }
        if (stockRow.sixtyDayPct() >= 15) {
            reasons.add("中期趋势保持上行");
        }
        if (qualityScore >= 75) {
            reasons.add("基础质量分较高");
        }
        if (reasons.isEmpty()) {
            reasons.add("综合因子分数居前");
        }

        return new StockOpportunity(
            stockRow.stockCode(),
            stockRow.market(),
            stockRow.stockName(),
            stockRow.industryName(),
            stockRow.regionName(),
            stockRow.conceptName(),
            stockRow.latestPrice(),
            stockRow.changePct(),
            stockRow.changeAmount(),
            stockRow.dealAmount(),
            stockRow.turnoverRatePct(),
            stockRow.volumeRatio(),
            stockRow.netInflow(),
            stockRow.marketCap(),
            stockRow.peTtm(),
            round1(finalScore),
            round1(trendScore),
            round1(capitalScore),
            round1(sectorScore),
            round1(liquidityScore),
            round1(qualityScore),
            round1(riskPenalty),
            round2(sectorChangePct),
            round2(sectorBreadth * 100),
            round0(sectorNetInflow),
            reasons,
            riskTags
        );
    }

    private List<String> buildRiskTags(StockRow stockRow, IndustryRow industryRow) {
        List<String> riskTags = new ArrayList<>();
        if (containsRiskFlag(stockRow.stockName())) {
            riskTags.add("ST风险");
        }
        if (stockRow.peTtm() <= 0) {
            riskTags.add("TTM亏损");
        } else if (stockRow.peTtm() > 120) {
            riskTags.add("估值偏高");
        }
        if (stockRow.pb() > 12) {
            riskTags.add("PB偏高");
        }
        if (stockRow.marketCap() > 0 && stockRow.marketCap() < 3_000_000_000d) {
            riskTags.add("小市值波动");
        }
        if (stockRow.amplitudePct() > 12) {
            riskTags.add("振幅过大");
        }
        if (stockRow.turnoverRatePct() > 25) {
            riskTags.add("换手过热");
        }
        if (stockRow.changePct() >= 9.8) {
            riskTags.add("涨停附近");
        }
        if (stockRow.changePct() <= -9.8) {
            riskTags.add("跌停附近");
        }
        if (industryRow == null) {
            riskTags.add("行业映射缺失");
        }
        return riskTags;
    }

    private Map<String, Object> toCandidatePayload(StockOpportunity opportunity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stockCode", opportunity.stockCode());
        payload.put("market", opportunity.market());
        payload.put("stockName", opportunity.stockName());
        payload.put("industryName", opportunity.industryName());
        payload.put("regionName", opportunity.regionName());
        payload.put("conceptName", opportunity.conceptName());
        payload.put("latestPrice", round2(opportunity.latestPrice()));
        payload.put("changePct", round2(opportunity.changePct()));
        payload.put("changeAmount", round2(opportunity.changeAmount()));
        payload.put("dealAmount", round0(opportunity.dealAmount()));
        payload.put("turnoverRatePct", round2(opportunity.turnoverRatePct()));
        payload.put("volumeRatio", round2(opportunity.volumeRatio()));
        payload.put("netInflow", round0(opportunity.netInflow()));
        payload.put("marketCap", round0(opportunity.marketCap()));
        payload.put("peTtm", round2(opportunity.peTtm()));
        payload.put("score", round1(opportunity.score()));
        payload.put("qualityScore", opportunity.qualityScore());

        Map<String, Object> scoreDetail = new LinkedHashMap<>();
        scoreDetail.put("trend", opportunity.trendScore());
        scoreDetail.put("capital", opportunity.capitalScore());
        scoreDetail.put("sector", opportunity.sectorScore());
        scoreDetail.put("liquidity", opportunity.liquidityScore());
        scoreDetail.put("quality", opportunity.qualityScore());
        scoreDetail.put("riskPenalty", opportunity.riskPenalty());
        payload.put("scoreDetail", scoreDetail);

        payload.put("sectorChangePct", opportunity.sectorChangePct());
        payload.put("sectorBreadthPct", opportunity.sectorBreadthPct());
        payload.put("sectorNetInflow", opportunity.sectorNetInflow());
        payload.put("reasons", opportunity.reasons());
        payload.put("riskTags", opportunity.riskTags());
        payload.put("riskLevel", resolveRiskLevel(opportunity.riskTags()));
        payload.put("attentionLevel", resolveAttentionLevel(opportunity.score()));
        return payload;
    }

    private Map<String, Object> toIndexPayload(MarketIndex index) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("label", index.label());
        payload.put("code", index.code());
        payload.put("latestPrice", round2(index.latestPrice()));
        payload.put("changePct", round2(index.changePct()));
        payload.put("changeAmount", round2(index.changeAmount()));
        payload.put("open", round2(index.openPrice()));
        payload.put("high", round2(index.highPrice()));
        payload.put("low", round2(index.lowPrice()));
        payload.put("prevClose", round2(index.prevClose()));
        payload.put("amount", round0(index.dealAmount()));
        return payload;
    }

    private Map<String, Object> toIndustryPayload(IndustryRow industryRow) {
        int total = industryRow.advanceCount() + industryRow.declineCount() + industryRow.flatCount();
        double advanceRatio = total > 0 ? (double) industryRow.advanceCount() / total : 0;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("industryCode", industryRow.industryCode());
        payload.put("industryName", industryRow.industryName());
        payload.put("changePct", round2(industryRow.changePct()));
        payload.put("netInflow", round0(industryRow.netInflow()));
        payload.put("advanceCount", industryRow.advanceCount());
        payload.put("declineCount", industryRow.declineCount());
        payload.put("flatCount", industryRow.flatCount());
        payload.put("advanceRatio", round2(advanceRatio * 100));
        payload.put("leaderName", industryRow.leaderName());
        payload.put("leaderCode", industryRow.leaderCode());
        return payload;
    }

    private List<String> buildWarnings(
        StockPoolSnapshot stockPoolSnapshot,
        IndustrySnapshot industrySnapshot,
        MarketIndexBundle marketIndexBundle
    ) {
        List<String> warnings = new ArrayList<>();
        if (stockPoolSnapshot.missingPageCount() > 0) {
            warnings.add("股票池缺少 " + stockPoolSnapshot.missingPageCount() + " 页数据，候选结果可能不完整");
        }
        if (stockPoolSnapshot.oldestFetchedAt() != null && stockPoolSnapshot.latestFetchedAt() != null) {
            long spanMinutes = Duration.between(stockPoolSnapshot.oldestFetchedAt(), stockPoolSnapshot.latestFetchedAt()).toMinutes();
            if (spanMinutes >= 20) {
                warnings.add("股票池分页快照跨越 " + spanMinutes + " 分钟，当前分析可能存在页级别时间差");
            }
        }
        if (industrySnapshot.rows().isEmpty()) {
            warnings.add("行业快照为空，当前评分未充分使用板块强弱因子");
        }
        if (marketIndexBundle.indices().isEmpty()) {
            warnings.add("大盘指数抓取失败，市场情绪分数退化为本地股票池估算");
        }
        return warnings;
    }

    private boolean containsRiskFlag(String stockName) {
        if (stockName == null) {
            return false;
        }
        return stockName.toUpperCase(Locale.ROOT).contains("ST");
    }

    private String resolveRiskLevel(List<String> riskTags) {
        if (riskTags == null || riskTags.isEmpty()) {
            return "低";
        }
        if (riskTags.size() >= 3 || riskTags.contains("ST风险")) {
            return "高";
        }
        return "中";
    }

    private String resolveAttentionLevel(double score) {
        if (score >= 80) {
            return "高关注";
        }
        if (score >= 70) {
            return "重点跟踪";
        }
        if (score >= 60) {
            return "观察";
        }
        return "一般";
    }

    private String resolveMarketTone(double sentimentScore) {
        if (sentimentScore >= 75) {
            return "强势";
        }
        if (sentimentScore >= 60) {
            return "偏强";
        }
        if (sentimentScore >= 45) {
            return "中性";
        }
        if (sentimentScore >= 30) {
            return "偏弱";
        }
        return "弱势";
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
        return node == null || node.isNull() ? "" : node.asText("");
    }

    private Integer intValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.canConvertToInt() ? node.asInt() : null;
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

    private double normalize(double value, double min, double max) {
        if (max <= min) {
            return 0;
        }
        return clamp((value - min) / (max - min), 0, 1);
    }

    private double scorePreferredRange(double value, double preferredMin, double preferredMax, double extremeMax) {
        if (value <= 0) {
            return 0;
        }
        if (value < preferredMin) {
            return normalize(value, 0, preferredMin);
        }
        if (value <= preferredMax) {
            return 1;
        }
        if (value >= extremeMax) {
            return 0;
        }
        return 1 - normalize(value, preferredMax, extremeMax);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (Double value : values) {
            total += value == null ? 0 : value;
        }
        return total / values.size();
    }

    private LocalDateTime min(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isBefore(right) ? left : right;
    }

    private LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private String toText(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private double round0(double value) {
        return Math.round(value);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record IndexDefinition(String label, String code, String secId) {
    }

    private record CachedIndexBundle(MarketIndexBundle bundle, LocalDateTime expiresAt) {
    }

    private record StockPoolSnapshot(
        List<StockRow> rows,
        LocalDateTime referenceFetchedAt,
        LocalDateTime oldestFetchedAt,
        LocalDateTime latestFetchedAt,
        int pageCount,
        int missingPageCount
    ) {
    }

    private record IndustrySnapshot(List<IndustryRow> rows, LocalDateTime fetchedAt) {
    }

    private record MarketIndexBundle(List<MarketIndex> indices, LocalDateTime fetchedAt) {
    }

    private record StockRow(
        String stockCode,
        Integer market,
        String stockName,
        double latestPrice,
        double changePct,
        double changeAmount,
        double dealAmount,
        double amplitudePct,
        double turnoverRatePct,
        double volumeRatio,
        double netInflow,
        double sixtyDayPct,
        double yearToDatePct,
        double marketCap,
        double circulatingMarketCap,
        double pb,
        double peTtm,
        String industryName,
        String regionName,
        String conceptName
    ) {
    }

    private record IndustryRow(
        String industryCode,
        String industryName,
        double changePct,
        double netInflow,
        int advanceCount,
        int declineCount,
        int flatCount,
        String leaderName,
        String leaderCode
    ) {
    }

    private record MarketIndex(
        String label,
        String code,
        String secId,
        double latestPrice,
        double changePct,
        double changeAmount,
        double openPrice,
        double highPrice,
        double lowPrice,
        double prevClose,
        double volume,
        double dealAmount
    ) {
    }

    private record MarketBreadth(
        int advanceCount,
        int declineCount,
        int flatCount,
        int limitUpCount,
        int limitDownCount,
        double averageChangePct,
        double sentimentScore,
        double advanceRatio
    ) {
    }

    private record StockOpportunity(
        String stockCode,
        Integer market,
        String stockName,
        String industryName,
        String regionName,
        String conceptName,
        double latestPrice,
        double changePct,
        double changeAmount,
        double dealAmount,
        double turnoverRatePct,
        double volumeRatio,
        double netInflow,
        double marketCap,
        double peTtm,
        double score,
        double trendScore,
        double capitalScore,
        double sectorScore,
        double liquidityScore,
        double qualityScore,
        double riskPenalty,
        double sectorChangePct,
        double sectorBreadthPct,
        double sectorNetInflow,
        List<String> reasons,
        List<String> riskTags
    ) {
    }
}
