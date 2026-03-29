package com.example.houduan.service;

import com.example.houduan.entity.CrawlJobLogEntity;
import com.example.houduan.entity.IndustryBaseResponseEntity;
import com.example.houduan.entity.StockKlineResponseEntity;
import com.example.houduan.entity.StockPoolResponseEntity;
import com.example.houduan.entity.StockRealResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EastMoneyCollectorService {

    private final AShareMarketClockService marketClockService;
    private final EastMoneyApiClient eastMoneyApiClient;
    private final EastMoneyStorageService eastMoneyStorageService;
    private final CrawlJobLogService crawlJobLogService;
    private final int maxStockPoolPages;
    private final int stockPoolPageSize;
    private final int stockPoolPauseAfterPages;
    private final long stockPoolPauseMs;
    private final int stockDetailPauseAfterSymbols;
    private final long stockDetailPauseMs;
    private final int repairMaxRounds;
    private final long repairRoundPauseMs;
    private final Duration industryBaseTradingInterval;
    private final Duration stockPoolTradingInterval;
    private final Duration integrityRepairTradingInterval;
    private final LocalTime industryBaseDailyRunTime;
    private final LocalTime stockPoolDailyRunTime;
    private final LocalTime industryKlineDailyRunTime;
    private final LocalTime stockDetailDailyRunTime;
    private final LocalTime integrityRepairDailyRunTime;
    private final Map<String, AtomicBoolean> runningJobs = new ConcurrentHashMap<>();

    public EastMoneyCollectorService(
        AShareMarketClockService marketClockService,
        EastMoneyApiClient eastMoneyApiClient,
        EastMoneyStorageService eastMoneyStorageService,
        CrawlJobLogService crawlJobLogService,
        @Value("${collector.max-stock-pool-pages:1}") int maxStockPoolPages,
        @Value("${collector.stock-pool.page-size:100}") int stockPoolPageSize,
        @Value("${collector.stock-pool.pause-after-pages:10}") int stockPoolPauseAfterPages,
        @Value("${collector.stock-pool.pause-ms:5000}") long stockPoolPauseMs,
        @Value("${collector.stock-detail.pause-after-symbols:40}") int stockDetailPauseAfterSymbols,
        @Value("${collector.stock-detail.pause-ms:15000}") long stockDetailPauseMs,
        @Value("${collector.repair.max-rounds:2}") int repairMaxRounds,
        @Value("${collector.repair.round-pause-ms:3000}") long repairRoundPauseMs,
        @Value("${collector.industry-base.trading-interval-minutes:5}") long industryBaseTradingIntervalMinutes,
        @Value("${collector.stock-pool.trading-interval-minutes:5}") long stockPoolTradingIntervalMinutes,
        @Value("${collector.integrity-repair.trading-interval-minutes:30}") long integrityRepairTradingIntervalMinutes,
        @Value("${collector.industry-base.daily-run-time:15:05}") String industryBaseDailyRunTime,
        @Value("${collector.stock-pool.daily-run-time:15:06}") String stockPoolDailyRunTime,
        @Value("${collector.industry-kline.daily-run-time:15:20}") String industryKlineDailyRunTime,
        @Value("${collector.stock-detail.daily-run-time:15:35}") String stockDetailDailyRunTime,
        @Value("${collector.integrity-repair.daily-run-time:15:50}") String integrityRepairDailyRunTime
    ) {
        this.marketClockService = marketClockService;
        this.eastMoneyApiClient = eastMoneyApiClient;
        this.eastMoneyStorageService = eastMoneyStorageService;
        this.crawlJobLogService = crawlJobLogService;
        this.maxStockPoolPages = maxStockPoolPages;
        this.stockPoolPageSize = stockPoolPageSize;
        this.stockPoolPauseAfterPages = stockPoolPauseAfterPages;
        this.stockPoolPauseMs = stockPoolPauseMs;
        this.stockDetailPauseAfterSymbols = stockDetailPauseAfterSymbols;
        this.stockDetailPauseMs = stockDetailPauseMs;
        this.repairMaxRounds = Math.max(1, repairMaxRounds);
        this.repairRoundPauseMs = Math.max(0, repairRoundPauseMs);
        this.industryBaseTradingInterval = Duration.ofMinutes(Math.max(1, industryBaseTradingIntervalMinutes));
        this.stockPoolTradingInterval = Duration.ofMinutes(Math.max(1, stockPoolTradingIntervalMinutes));
        this.integrityRepairTradingInterval = Duration.ofMinutes(Math.max(5, integrityRepairTradingIntervalMinutes));
        this.industryBaseDailyRunTime = parseTime(industryBaseDailyRunTime, LocalTime.of(15, 5));
        this.stockPoolDailyRunTime = parseTime(stockPoolDailyRunTime, LocalTime.of(15, 6));
        this.industryKlineDailyRunTime = parseTime(industryKlineDailyRunTime, LocalTime.of(15, 20));
        this.stockDetailDailyRunTime = parseTime(stockDetailDailyRunTime, LocalTime.of(15, 35));
        this.integrityRepairDailyRunTime = parseTime(integrityRepairDailyRunTime, LocalTime.of(15, 50));
    }

    @Scheduled(cron = "${collector.industry-base.cron}")
    public void collectIndustryBase() {
        if (!shouldRunTradingAware(latestIndustryBaseFetchedAt(), industryBaseTradingInterval, industryBaseDailyRunTime)) {
            return;
        }

        runScheduledExclusively("industry-base-schedule", () ->
            runJob("industry-base", null, () -> {
                IndustryBaseResponseEntity saved = eastMoneyStorageService.saveIndustryBase(eastMoneyApiClient.fetchIndustryBase());
                return saved.getItemCount();
            })
        );
    }

    @Scheduled(cron = "${collector.stock-pool.cron}")
    public void collectStockPool() {
        if (!shouldRunTradingAware(latestStockPoolFetchedAt(), stockPoolTradingInterval, stockPoolDailyRunTime)) {
            return;
        }

        runScheduledExclusively("stock-pool-schedule", this::doCollectStockPool);
    }

    @Scheduled(cron = "${collector.industry-kline.cron}")
    public void collectIndustryKlines() {
        if (!shouldRunDailyAfterClose(latestSuccessfulJobAt("industry-kline-batch"), industryKlineDailyRunTime)) {
            return;
        }

        runScheduledExclusively("industry-kline-schedule", this::doCollectIndustryKlines);
    }

    @Scheduled(cron = "${collector.stock-detail.cron}")
    public void collectStockDetails() {
        if (!shouldRunDailyAfterClose(latestSuccessfulJobAt("stock-detail-batch"), stockDetailDailyRunTime)) {
            return;
        }

        runScheduledExclusively("stock-detail-schedule", this::doCollectStockDetails);
    }

    @Scheduled(cron = "${collector.integrity-repair.cron:45 */10 * * * *}")
    public void repairDataIntegrity() {
        if (!shouldRunTradingAware(latestSuccessfulJobAt("integrity-repair"), integrityRepairTradingInterval, integrityRepairDailyRunTime)) {
            return;
        }

        runScheduledExclusively("integrity-repair-schedule", () -> {
            String conflictingJob = findRunningJob(
                "industry-base-schedule",
                "stock-pool-schedule",
                "industry-kline-schedule",
                "stock-detail-schedule"
            );
            if (conflictingJob != null) {
                System.out.println("Skip integrity repair because another collector is still running: " + conflictingJob);
                return;
            }
            doRepairDataIntegrity();
        });
    }

    private void doCollectStockPool() {
        LocalDateTime batchStartedAt = marketClockService.now();
        String firstPageRawJson;
        try {
            firstPageRawJson = eastMoneyApiClient.fetchStockPool(1);
        } catch (Exception e) {
            crawlJobLogService.log("stock-pool", "1", batchStartedAt, false, null, truncate(e.getMessage()));
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

        RepairSummary repairSummary = repairStockPoolPages(
            totalPages,
            batchStartedAt,
            null,
            "stock-pool-repair"
        );

        runJob("stock-pool", "1", batchStartedAt, () -> {
            StockPoolResponseEntity saved = eastMoneyStorageService.saveStockPool(1, firstPageRawJson);
            return saved.getItemCount();
        });

        logRepairSummaryIfNeeded("stock-pool-check", batchStartedAt, repairSummary);
    }

    private void doCollectIndustryKlines() {
        LocalDateTime batchStartedAt = marketClockService.now();
        Optional<IndustryBaseResponseEntity> latestIndustryBase = eastMoneyStorageService.findLatestIndustryBase();
        if (latestIndustryBase.isEmpty()) {
            crawlJobLogService.log("industry-kline-batch", "all", batchStartedAt, false, 0, "No industry-base snapshot available");
            return;
        }

        IndustryBaseResponseEntity industryBase = latestIndustryBase.get();
        List<String> industryCodes = eastMoneyStorageService.extractIndustryCodes(industryBase.getRawJson());

        for (String industryCode : industryCodes) {
            runJob("industry-kline", industryCode, () -> {
                eastMoneyStorageService.saveIndustryKline(industryCode, eastMoneyApiClient.fetchIndustryKline(industryCode));
                return null;
            });
        }

        RepairSummary repairSummary = repairIndustryKlines(
            industryCodes,
            industryBase.getFetchedAt(),
            "industry-kline-repair"
        );
        logRepairSummaryIfNeeded("industry-kline-check", batchStartedAt, repairSummary);
        crawlJobLogService.log(
            "industry-kline-batch",
            "all",
            batchStartedAt,
            repairSummary.remainingCount() == 0,
            industryCodes.size(),
            truncate("codes=" + industryCodes.size() + "; " + repairSummary.describe("codes"))
        );
    }

    private void doCollectStockDetails() {
        LocalDateTime batchStartedAt = marketClockService.now();
        Optional<StockPoolSnapshot> stockPoolSnapshot = loadLatestStockPoolSnapshot();
        if (stockPoolSnapshot.isEmpty()) {
            crawlJobLogService.log("stock-detail-batch", "all", batchStartedAt, false, 0, "No stock-pool snapshot available");
            return;
        }

        Map<String, EastMoneyStorageService.StockTarget> stockTargetsByCode = loadStockTargets(stockPoolSnapshot.get());
        if (stockTargetsByCode.isEmpty()) {
            crawlJobLogService.log("stock-detail-batch", "all", batchStartedAt, false, 0, "No stock targets extracted from stock-pool snapshot");
            return;
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

        LocalDateTime referenceFetchedAt = stockPoolSnapshot.get().referenceFetchedAt();
        RepairSummary stockRealRepair = repairStockPayload(
            stockTargetsByCode.values(),
            referenceFetchedAt,
            StockPayloadType.REAL,
            "stock-real-repair"
        );
        RepairSummary stockKlineRepair = repairStockPayload(
            stockTargetsByCode.values(),
            referenceFetchedAt,
            StockPayloadType.KLINE,
            "stock-kline-repair"
        );
        logCombinedRepairSummaryIfNeeded(
            "stock-detail-check",
            batchStartedAt,
            stockRealRepair,
            stockKlineRepair
        );

        int repairedCount = stockRealRepair.repairedCount() + stockKlineRepair.repairedCount();
        int remainingCount = stockRealRepair.remainingCount() + stockKlineRepair.remainingCount();
        String message = "symbols=" + stockTargetsByCode.size()
            + "; stock-real=" + stockRealRepair.describe("symbols")
            + "; stock-kline=" + stockKlineRepair.describe("symbols");

        crawlJobLogService.log(
            "stock-detail-batch",
            "all",
            batchStartedAt,
            remainingCount == 0,
            stockTargetsByCode.size() + repairedCount,
            truncate(message)
        );
    }

    private void doRepairDataIntegrity() {
        LocalDateTime startedAt = marketClockService.now();

        RepairSummary stockPoolRepair = repairStockPoolFromLatestSnapshot();
        RepairSummary industryRepair = repairIndustryKlinesFromLatestSnapshot();

        Optional<StockPoolSnapshot> stockPoolSnapshot = loadLatestStockPoolSnapshot();
        RepairSummary stockRealRepair = RepairSummary.empty();
        RepairSummary stockKlineRepair = RepairSummary.empty();

        if (stockPoolSnapshot.isPresent()) {
            Map<String, EastMoneyStorageService.StockTarget> stockTargetsByCode = loadStockTargets(stockPoolSnapshot.get());
            if (!stockTargetsByCode.isEmpty()) {
                stockRealRepair = repairStockPayload(
                    stockTargetsByCode.values(),
                    stockPoolSnapshot.get().referenceFetchedAt(),
                    StockPayloadType.REAL,
                    "stock-real-repair"
                );
                stockKlineRepair = repairStockPayload(
                    stockTargetsByCode.values(),
                    stockPoolSnapshot.get().referenceFetchedAt(),
                    StockPayloadType.KLINE,
                    "stock-kline-repair"
                );
            }
        }

        int repairedCount = stockPoolRepair.repairedCount()
            + industryRepair.repairedCount()
            + stockRealRepair.repairedCount()
            + stockKlineRepair.repairedCount();
        int remainingCount = stockPoolRepair.remainingCount()
            + industryRepair.remainingCount()
            + stockRealRepair.remainingCount()
            + stockKlineRepair.remainingCount();

        String message = "stock-pool: " + stockPoolRepair.describe("pages")
            + "; industry-kline: " + industryRepair.describe("codes")
            + "; stock-real: " + stockRealRepair.describe("symbols")
            + "; stock-kline: " + stockKlineRepair.describe("symbols");

        crawlJobLogService.log(
            "integrity-repair",
            "all",
            startedAt,
            remainingCount == 0,
            repairedCount,
            truncate(message)
        );
    }

    private RepairSummary repairStockPoolFromLatestSnapshot() {
        Optional<StockPoolSnapshot> latestSnapshot = loadLatestStockPoolSnapshot();
        if (latestSnapshot.isEmpty()) {
            doCollectStockPool();
            return RepairSummary.empty();
        }

        StockPoolSnapshot snapshot = latestSnapshot.get();
        RepairSummary repairSummary = repairStockPoolPages(
            snapshot.pageLimit(),
            snapshot.snapshotStartedAt(),
            snapshot.referenceFetchedAt(),
            "stock-pool-repair"
        );

        if (repairSummary.repairedCount() > 0) {
            runJob("stock-pool-repair", "1", snapshot.snapshotStartedAt(), () -> {
                StockPoolResponseEntity saved = eastMoneyStorageService.saveStockPool(1, snapshot.firstPageRawJson());
                return saved.getItemCount();
            });
        }

        return repairSummary;
    }

    private RepairSummary repairIndustryKlinesFromLatestSnapshot() {
        Optional<IndustryBaseResponseEntity> latestIndustryBase = eastMoneyStorageService.findLatestIndustryBase();
        if (latestIndustryBase.isEmpty()) {
            return RepairSummary.empty();
        }

        IndustryBaseResponseEntity entity = latestIndustryBase.get();
        return repairIndustryKlines(
            eastMoneyStorageService.extractIndustryCodes(entity.getRawJson()),
            entity.getFetchedAt(),
            "industry-kline-repair"
        );
    }

    private RepairSummary repairIndustryKlines(
        List<String> industryCodes,
        LocalDateTime referenceFetchedAt,
        String repairJobName
    ) {
        if (industryCodes.isEmpty() || referenceFetchedAt == null) {
            return RepairSummary.empty();
        }

        List<String> pendingCodes = new ArrayList<>();
        for (String industryCode : industryCodes) {
            if (!isIndustryKlineFresh(industryCode, referenceFetchedAt)) {
                pendingCodes.add(industryCode);
            }
        }

        int initialMissing = pendingCodes.size();
        int repairedCount = 0;

        for (int round = 1; round <= repairMaxRounds && !pendingCodes.isEmpty(); round++) {
            List<String> nextRound = new ArrayList<>();
            for (String industryCode : pendingCodes) {
                boolean success = runJob(repairJobName, industryCode, () -> {
                    eastMoneyStorageService.saveIndustryKline(industryCode, eastMoneyApiClient.fetchIndustryKline(industryCode));
                    return null;
                });
                if (success) {
                    repairedCount++;
                } else {
                    nextRound.add(industryCode);
                }
            }
            pendingCodes = nextRound;
            pauseBetweenRepairRounds(round, pendingCodes.isEmpty());
        }

        return new RepairSummary(industryCodes.size(), initialMissing, repairedCount, pendingCodes.size());
    }

    private RepairSummary repairStockPayload(
        Collection<EastMoneyStorageService.StockTarget> stockTargets,
        LocalDateTime referenceFetchedAt,
        StockPayloadType payloadType,
        String repairJobName
    ) {
        if (stockTargets.isEmpty() || referenceFetchedAt == null) {
            return RepairSummary.empty();
        }

        List<EastMoneyStorageService.StockTarget> pendingTargets = new ArrayList<>();
        for (EastMoneyStorageService.StockTarget stockTarget : stockTargets) {
            if (!isStockPayloadFresh(stockTarget.stockCode(), referenceFetchedAt, payloadType)) {
                pendingTargets.add(stockTarget);
            }
        }

        int initialMissing = pendingTargets.size();
        int repairedCount = 0;

        for (int round = 1; round <= repairMaxRounds && !pendingTargets.isEmpty(); round++) {
            List<EastMoneyStorageService.StockTarget> nextRound = new ArrayList<>();
            for (EastMoneyStorageService.StockTarget stockTarget : pendingTargets) {
                boolean success = runJob(repairJobName, stockTarget.stockCode(), () -> {
                    if (payloadType == StockPayloadType.REAL) {
                        eastMoneyStorageService.saveStockReal(
                            stockTarget.stockCode(),
                            eastMoneyApiClient.fetchStockReal(stockTarget.stockCode(), stockTarget.market())
                        );
                    } else {
                        eastMoneyStorageService.saveStockKline(
                            stockTarget.stockCode(),
                            eastMoneyApiClient.fetchStockKline(stockTarget.stockCode(), stockTarget.market())
                        );
                    }
                    return null;
                });
                if (success) {
                    repairedCount++;
                } else {
                    nextRound.add(stockTarget);
                }
            }
            pendingTargets = nextRound;
            pauseBetweenRepairRounds(round, pendingTargets.isEmpty());
        }

        return new RepairSummary(stockTargets.size(), initialMissing, repairedCount, pendingTargets.size());
    }

    private RepairSummary repairStockPoolPages(
        int totalPages,
        LocalDateTime snapshotStartedAt,
        LocalDateTime referenceFetchedAt,
        String repairJobName
    ) {
        if (totalPages <= 1 || snapshotStartedAt == null) {
            return new RepairSummary(Math.max(totalPages, 0), 0, 0, 0);
        }

        List<Integer> pendingPages = new ArrayList<>();
        for (int pageNo = 2; pageNo <= totalPages; pageNo++) {
            if (!isStockPoolPageWithinSnapshot(pageNo, snapshotStartedAt, referenceFetchedAt)) {
                pendingPages.add(pageNo);
            }
        }

        int initialMissing = pendingPages.size();
        int repairedCount = 0;

        for (int round = 1; round <= repairMaxRounds && !pendingPages.isEmpty(); round++) {
            List<Integer> nextRound = new ArrayList<>();
            for (Integer pageNo : pendingPages) {
                boolean success = runJob(repairJobName, String.valueOf(pageNo), () -> {
                    StockPoolResponseEntity saved = eastMoneyStorageService.saveStockPool(pageNo, eastMoneyApiClient.fetchStockPool(pageNo));
                    pauseIfNeeded("stock-pool-repair", pageNo, stockPoolPauseAfterPages, stockPoolPauseMs);
                    return saved.getItemCount();
                });
                if (success) {
                    repairedCount++;
                } else {
                    nextRound.add(pageNo);
                }
            }
            pendingPages = nextRound;
            pauseBetweenRepairRounds(round, pendingPages.isEmpty());
        }

        return new RepairSummary(totalPages - 1, initialMissing, repairedCount, pendingPages.size());
    }

    private Optional<StockPoolSnapshot> loadLatestStockPoolSnapshot() {
        Optional<StockPoolResponseEntity> latestFirstPage = eastMoneyStorageService.findLatestStockPool(1);
        if (latestFirstPage.isEmpty()) {
            return Optional.empty();
        }

        StockPoolResponseEntity firstPage = latestFirstPage.get();
        int pageLimit = resolvePageLimit(firstPage.getRawJson());
        LocalDateTime snapshotStartedAt = crawlJobLogService.findLatestSuccessfulLog("stock-pool", "1")
            .map(CrawlJobLogEntity::getStartedAt)
            .orElse(firstPage.getFetchedAt());

        return Optional.of(new StockPoolSnapshot(
            firstPage.getRawJson(),
            pageLimit,
            snapshotStartedAt,
            firstPage.getFetchedAt()
        ));
    }

    private Map<String, EastMoneyStorageService.StockTarget> loadStockTargets(StockPoolSnapshot stockPoolSnapshot) {
        Map<String, EastMoneyStorageService.StockTarget> stockTargetsByCode = new LinkedHashMap<>();
        for (int pageNo = 1; pageNo <= stockPoolSnapshot.pageLimit(); pageNo++) {
            List<EastMoneyStorageService.StockTarget> stockTargets = eastMoneyStorageService.findLatestStockPoolAtOrBefore(
                    pageNo,
                    stockPoolSnapshot.referenceFetchedAt()
                )
                .map(StockPoolResponseEntity::getRawJson)
                .map(eastMoneyStorageService::extractStockTargets)
                .orElse(Collections.emptyList());

            for (EastMoneyStorageService.StockTarget stockTarget : stockTargets) {
                stockTargetsByCode.putIfAbsent(stockTarget.stockCode(), stockTarget);
            }
        }
        return stockTargetsByCode;
    }

    private boolean isStockPoolPageWithinSnapshot(
        int pageNo,
        LocalDateTime snapshotStartedAt,
        LocalDateTime referenceFetchedAt
    ) {
        Optional<StockPoolResponseEntity> latestPage = eastMoneyStorageService.findLatestStockPool(pageNo);
        if (latestPage.isEmpty()) {
            return false;
        }

        LocalDateTime fetchedAt = latestPage.get().getFetchedAt();
        if (fetchedAt.isBefore(snapshotStartedAt)) {
            return false;
        }
        return referenceFetchedAt == null || !fetchedAt.isAfter(referenceFetchedAt);
    }

    private boolean isIndustryKlineFresh(String industryCode, LocalDateTime referenceFetchedAt) {
        return eastMoneyStorageService.findLatestIndustryKline(industryCode)
            .map(entity -> !entity.getFetchedAt().isBefore(referenceFetchedAt))
            .orElse(false);
    }

    private boolean isStockPayloadFresh(String stockCode, LocalDateTime referenceFetchedAt, StockPayloadType payloadType) {
        if (payloadType == StockPayloadType.REAL) {
            return eastMoneyStorageService.findLatestStockReal(stockCode)
                .map(StockRealResponseEntity::getFetchedAt)
                .map(fetchedAt -> !fetchedAt.isBefore(referenceFetchedAt))
                .orElse(false);
        }
        return eastMoneyStorageService.findLatestStockKline(stockCode)
            .map(StockKlineResponseEntity::getFetchedAt)
            .map(fetchedAt -> !fetchedAt.isBefore(referenceFetchedAt))
            .orElse(false);
    }

    private LocalDateTime latestIndustryBaseFetchedAt() {
        return eastMoneyStorageService.findLatestIndustryBase()
            .map(IndustryBaseResponseEntity::getFetchedAt)
            .orElse(null);
    }

    private LocalDateTime latestStockPoolFetchedAt() {
        return eastMoneyStorageService.findLatestStockPool(1)
            .map(StockPoolResponseEntity::getFetchedAt)
            .orElse(null);
    }

    private LocalDateTime latestSuccessfulJobAt(String jobName) {
        return crawlJobLogService.findLatestSuccessfulLog(jobName)
            .map(this::resolveLogMoment)
            .orElse(null);
    }

    private LocalDateTime resolveLogMoment(CrawlJobLogEntity entity) {
        return entity.getFinishedAt() != null ? entity.getFinishedAt() : entity.getStartedAt();
    }

    private boolean shouldRunTradingAware(
        LocalDateTime lastSuccessAt,
        Duration tradingInterval,
        LocalTime dailyRunTime
    ) {
        LocalDateTime now = marketClockService.now();
        if (marketClockService.isTradingSession(now)) {
            return lastSuccessAt == null || !lastSuccessAt.plus(tradingInterval).isAfter(now);
        }

        if (!marketClockService.isTradingDay(now.toLocalDate())) {
            return false;
        }

        LocalDateTime todayDailyRunAt = now.toLocalDate().atTime(dailyRunTime);
        return !now.isBefore(todayDailyRunAt)
            && (lastSuccessAt == null || lastSuccessAt.isBefore(todayDailyRunAt));
    }

    private boolean shouldRunDailyAfterClose(LocalDateTime lastSuccessAt, LocalTime dailyRunTime) {
        LocalDateTime now = marketClockService.now();
        if (!marketClockService.isTradingDay(now.toLocalDate()) || marketClockService.isTradingSession(now)) {
            return false;
        }

        LocalDateTime todayDailyRunAt = now.toLocalDate().atTime(dailyRunTime);
        return !now.isBefore(todayDailyRunAt)
            && (lastSuccessAt == null || lastSuccessAt.isBefore(todayDailyRunAt));
    }

    private LocalTime parseTime(String rawValue, LocalTime defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalTime.parse(rawValue.trim());
        } catch (Exception ignored) {
            return defaultValue;
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
            System.out.println("Skip scheduled job because previous run is still active: " + jobKey);
            return;
        }

        try {
            action.run();
        } finally {
            running.set(false);
        }
    }

    private String findRunningJob(String... jobKeys) {
        for (String jobKey : jobKeys) {
            AtomicBoolean running = runningJobs.get(jobKey);
            if (running != null && running.get()) {
                return jobKey;
            }
        }
        return null;
    }

    private void pauseIfNeeded(String jobName, int processedCount, int pauseAfterCount, long pauseMs) {
        if (pauseAfterCount <= 0 || pauseMs <= 0) {
            return;
        }
        if (processedCount > 0 && processedCount % pauseAfterCount == 0) {
            System.out.println("Pause " + jobName + " after processing " + processedCount + " items for " + pauseMs + "ms");
            sleepQuietly(pauseMs);
        }
    }

    private void pauseBetweenRepairRounds(int round, boolean finished) {
        if (!finished && round < repairMaxRounds && repairRoundPauseMs > 0) {
            sleepQuietly(repairRoundPauseMs);
        }
    }

    private boolean runJob(String jobName, String targetKey, CrawlAction action) {
        return runJob(jobName, targetKey, marketClockService.now(), action);
    }

    private boolean runJob(String jobName, String targetKey, LocalDateTime startedAt, CrawlAction action) {
        try {
            Integer recordCount = action.run();
            crawlJobLogService.log(jobName, targetKey, startedAt, true, recordCount, "OK");
            return true;
        } catch (Exception e) {
            crawlJobLogService.log(jobName, targetKey, startedAt, false, null, truncate(e.getMessage()));
            return false;
        }
    }

    private void logRepairSummaryIfNeeded(String jobName, LocalDateTime startedAt, RepairSummary repairSummary) {
        if (repairSummary.initialMissingCount() == 0 && repairSummary.remainingCount() == 0) {
            return;
        }

        crawlJobLogService.log(
            jobName,
            null,
            startedAt,
            repairSummary.remainingCount() == 0,
            repairSummary.repairedCount(),
            truncate(repairSummary.describe("targets"))
        );
    }

    private void logCombinedRepairSummaryIfNeeded(
        String jobName,
        LocalDateTime startedAt,
        RepairSummary firstSummary,
        RepairSummary secondSummary
    ) {
        if (firstSummary.initialMissingCount() == 0
            && firstSummary.remainingCount() == 0
            && secondSummary.initialMissingCount() == 0
            && secondSummary.remainingCount() == 0) {
            return;
        }

        String message = "stock-real: " + firstSummary.describe("symbols")
            + "; stock-kline: " + secondSummary.describe("symbols");
        int repairedCount = firstSummary.repairedCount() + secondSummary.repairedCount();
        int remainingCount = firstSummary.remainingCount() + secondSummary.remainingCount();

        crawlJobLogService.log(
            jobName,
            null,
            startedAt,
            remainingCount == 0,
            repairedCount,
            truncate(message)
        );
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

    private enum StockPayloadType {
        REAL,
        KLINE
    }

    private record StockPoolSnapshot(
        String firstPageRawJson,
        int pageLimit,
        LocalDateTime snapshotStartedAt,
        LocalDateTime referenceFetchedAt
    ) {
    }

    private record RepairSummary(
        int checkedCount,
        int initialMissingCount,
        int repairedCount,
        int remainingCount
    ) {

        static RepairSummary empty() {
            return new RepairSummary(0, 0, 0, 0);
        }

        String describe(String unitLabel) {
            return "checked=" + checkedCount
                + ", missing=" + initialMissingCount
                + ", repaired=" + repairedCount
                + ", remaining=" + remainingCount
                + ", unit=" + unitLabel;
        }
    }
}
