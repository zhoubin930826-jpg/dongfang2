package com.example.houduan.service;

import com.example.houduan.utils.ClaudeEastMoneyApiUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EastMoneyApiClient {

    public static final String INDUSTRY_BASE_TEMPLATE =
        "https://push2.eastmoney.com/api/qt/clist/get?pn=%s&pz=%s&po=1&np=1&fltt=2&invt=2&wbp2u=|0|0|0|web&fid=f3&fs=m:90+t:2+f:!50&fields=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f26,f22,f33,f11,f62,f128,f136,f115,f152,f124,f107,f104,f105,f106,f140,f141,f207,f208,f209,f222";
    private static final String INDUSTRY_KLINE_TEMPLATE =
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=90.%s&fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&klt=101&fqt=1&beg=0&end=20500101&smplmt=1209&lmt=1000000";
    private static final String STOCK_REAL_TEMPLATE =
        "https://push2.eastmoney.com/api/qt/stock/get?fields=f58,f734,f107,f57,f43,f59,f169,f170,f152,f177,f111,f46,f60,f44,f45,f47,f260,f48,f261,f279,f277,f278,f288,f19,f17,f531,f15,f13,f11,f20,f18,f16,f14,f12,f39,f37,f35,f33,f31,f40,f38,f36,f34,f32,f211,f212,f213,f214,f215,f210,f209,f208,f207,f206,f161,f49,f171,f50,f86,f84,f85,f168,f108,f116,f167,f164,f162,f163,f92,f71,f117,f292,f51,f52,f191,f192,f262,f294,f295,f269,f270,f256,f257,f285,f286,f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f22,f11,f62,f128,f136,f115,f152&secid=%s";
    private static final String STOCK_KLINE_TEMPLATE =
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&ut=1&klt=101&fqt=1&secid=%s&beg=0&end=20500000";
    private static final String STOCK_TRENDS_TEMPLATE =
        "https://push2his.eastmoney.com/api/qt/stock/trends2/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8&fields2=f51,f52,f53,f54,f55,f56,f57,f58&ut=fa5fd1943c7b386f172d6893dbfba10b&ndays=%s&iscr=0&iscca=0&secid=%s";
    private static final String STOCK_POOL_TEMPLATE =
        "https://push2.eastmoney.com/api/qt/clist/get?pn=%s&pz=%s&po=1&fs=m:1+t:2,m:1+t:23,m:0+t:6,m:0+t:80&fields=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f22,f11,f62,f100,f102,f103,f128,f136,f115,f152";
    private static final String MARKET_INDEX_TEMPLATE =
        "https://push2.eastmoney.com/api/qt/stock/get?fields=f57,f58,f43,f169,f170,f46,f44,f45,f60,f47,f48&secid=%s";

    private final ObjectMapper objectMapper;
    private final int industryBasePageSize;
    private final int stockPoolPageSize;

    public EastMoneyApiClient(
        ObjectMapper objectMapper,
        @Value("${collector.industry-base.page-size:100}") int industryBasePageSize,
        @Value("${collector.stock-pool.page-size:100}") int stockPoolPageSize
    ) {
        this.objectMapper = objectMapper;
        this.industryBasePageSize = industryBasePageSize;
        this.stockPoolPageSize = stockPoolPageSize;
    }

    public String fetchIndustryBase() {
        String firstPage = fetchIndustryBasePage(1);
        int totalPages = resolveIndustryBasePages(firstPage);
        if (totalPages <= 1) {
            return firstPage;
        }

        try {
            JsonNode firstRoot = objectMapper.readTree(firstPage);
            JsonNode dataNode = firstRoot.get("data");
            if (!(firstRoot instanceof ObjectNode rootObject) || !(dataNode instanceof ObjectNode dataObject)) {
                return firstPage;
            }

            ArrayNode mergedDiff = objectMapper.createArrayNode();
            appendDiffNodes(mergedDiff, dataObject.get("diff"));

            for (int pageNo = 2; pageNo <= totalPages; pageNo++) {
                appendDiffNodes(mergedDiff, readPath(fetchIndustryBasePage(pageNo), "data", "diff"));
            }

            dataObject.set("diff", mergedDiff);
            dataObject.put("total", readInt(dataObject.get("total"), mergedDiff.size()));
            return objectMapper.writeValueAsString(rootObject);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to merge industry-base pages", e);
        }
    }

    public String fetchIndustryKline(String industryCode) {
        return requireData(ClaudeEastMoneyApiUtil.fetchData(String.format(INDUSTRY_KLINE_TEMPLATE, industryCode)), "industry-kline:" + industryCode);
    }

    public String fetchStockReal(String stockCode, Integer market) {
        String secId = buildSecId(stockCode, market);
        return requireData(ClaudeEastMoneyApiUtil.fetchData(String.format(STOCK_REAL_TEMPLATE, secId)), "stock-real:" + secId);
    }

    public String fetchStockKline(String stockCode, Integer market) {
        String secId = buildSecId(stockCode, market);
        return requireData(ClaudeEastMoneyApiUtil.fetchData(String.format(STOCK_KLINE_TEMPLATE, secId)), "stock-kline:" + secId);
    }

    public String fetchStockTrends(String stockCode, Integer market, int days) {
        String secId = buildSecId(stockCode, market);
        int normalizedDays = Math.max(1, Math.min(days, 5));
        return requireData(
            ClaudeEastMoneyApiUtil.fetchData(String.format(STOCK_TRENDS_TEMPLATE, normalizedDays, secId)),
            "stock-trends:" + secId
        );
    }

    public String fetchStockPool(int pageNo) {
        return requireData(
            ClaudeEastMoneyApiUtil.fetchData(String.format(STOCK_POOL_TEMPLATE, pageNo, stockPoolPageSize)),
            "stock-pool:" + pageNo
        );
    }

    public String fetchMarketIndex(String secId) {
        return requireData(
            ClaudeEastMoneyApiUtil.fetchData(String.format(MARKET_INDEX_TEMPLATE, secId)),
            "market-index:" + secId
        );
    }

    public int getStockPoolPageSize() {
        return stockPoolPageSize;
    }

    private String buildSecId(String stockCode, Integer market) {
        String normalizedCode = normalizeStockCode(stockCode);
        int normalizedMarket = market == null ? inferMarket(normalizedCode) : normalizeMarket(market, normalizedCode);
        return normalizedMarket + "." + normalizedCode;
    }

    private String normalizeStockCode(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("stockCode must not be blank");
        }
        return stockCode.trim();
    }

    private int normalizeMarket(Integer market, String stockCode) {
        if (market == 0 || market == 1) {
            return market;
        }
        return inferMarket(stockCode);
    }

    private int inferMarket(String stockCode) {
        if (stockCode.startsWith("5") || stockCode.startsWith("6") || stockCode.startsWith("9")) {
            return 1;
        }
        return 0;
    }

    private String requireData(String rawJson, String jobName) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalStateException("No data returned for " + jobName);
        }
        return rawJson;
    }

    private String fetchIndustryBasePage(int pageNo) {
        return requireData(
            ClaudeEastMoneyApiUtil.fetchData(String.format(INDUSTRY_BASE_TEMPLATE, pageNo, industryBasePageSize)),
            "industry-base:" + pageNo
        );
    }

    private int resolveIndustryBasePages(String firstPageRawJson) {
        JsonNode diffNode = readPath(firstPageRawJson, "data", "diff");
        int currentPageSize = diffNode != null && diffNode.isArray() ? diffNode.size() : 0;
        int effectivePageSize = currentPageSize > 0 ? currentPageSize : industryBasePageSize;
        if (effectivePageSize <= 0) {
            effectivePageSize = 100;
        }

        int totalCount = readInt(readPath(firstPageRawJson, "data", "total"), effectivePageSize);
        return Math.max(1, (totalCount + effectivePageSize - 1) / effectivePageSize);
    }

    private void appendDiffNodes(ArrayNode target, JsonNode diffNode) {
        if (diffNode == null) {
            return;
        }
        if (diffNode.isArray()) {
            diffNode.forEach(target::add);
            return;
        }
        if (diffNode.isObject()) {
            diffNode.elements().forEachRemaining(target::add);
        }
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

    private int readInt(JsonNode node, int defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        return node.asInt(defaultValue);
    }
}
