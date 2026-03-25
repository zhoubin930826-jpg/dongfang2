package com.example.houduan.service;

import com.example.houduan.utils.ClaudeEastMoneyApiUtil;
import org.springframework.stereotype.Service;

@Service
public class EastMoneyApiClient {

    public static final String INDUSTRY_BASE_URL =
        "https://push2.eastmoney.com/api/qt/clist/get?pn=1&pz=2000&po=1&np=1&fltt=2&invt=2&wbp2u=|0|0|0|web&fid=f3&fs=m:90+t:2+f:!50&fields=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f26,f22,f33,f11,f62,f128,f136,f115,f152,f124,f107,f104,f105,f106,f140,f141,f207,f208,f209,f222";
    private static final String INDUSTRY_KLINE_TEMPLATE =
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=90.%s&fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&klt=101&fqt=1&beg=0&end=20500101&smplmt=1209&lmt=1000000";
    private static final String STOCK_REAL_TEMPLATE =
        "https://push2.eastmoney.com/api/qt/stock/get?fields=f58,f734,f107,f57,f43,f59,f169,f170,f152,f177,f111,f46,f60,f44,f45,f47,f260,f48,f261,f279,f277,f278,f288,f19,f17,f531,f15,f13,f11,f20,f18,f16,f14,f12,f39,f37,f35,f33,f31,f40,f38,f36,f34,f32,f211,f212,f213,f214,f215,f210,f209,f208,f207,f206,f161,f49,f171,f50,f86,f84,f85,f168,f108,f116,f167,f164,f162,f163,f92,f71,f117,f292,f51,f52,f191,f192,f262,f294,f295,f269,f270,f256,f257,f285,f286,f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f22,f11,f62,f128,f136,f115,f152&secid=1.%s";
    private static final String STOCK_KLINE_TEMPLATE =
        "https://push2his.eastmoney.com/api/qt/stock/kline/get?fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&ut=1&klt=101&fqt=1&secid=1.%s&beg=0&end=20500000";
    private static final String STOCK_POOL_TEMPLATE =
        "https://push2.eastmoney.com/api/qt/clist/get?pn=%s&pz=50&po=1&fid=f3&fs=m:1+t:2&fields=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f22,f11,f62,f100,f102,f103,f128,f136,f115,f152";

    public String fetchIndustryBase() {
        return requireData(ClaudeEastMoneyApiUtil.fetchData(INDUSTRY_BASE_URL), "industry-base");
    }

    public String fetchIndustryKline(String industryCode) {
        return requireData(ClaudeEastMoneyApiUtil.fetchData(String.format(INDUSTRY_KLINE_TEMPLATE, industryCode)), "industry-kline:" + industryCode);
    }

    public String fetchStockReal(String stockCode) {
        return requireData(ClaudeEastMoneyApiUtil.fetchData(String.format(STOCK_REAL_TEMPLATE, stockCode)), "stock-real:" + stockCode);
    }

    public String fetchStockKline(String stockCode) {
        return requireData(ClaudeEastMoneyApiUtil.fetchData(String.format(STOCK_KLINE_TEMPLATE, stockCode)), "stock-kline:" + stockCode);
    }

    public String fetchStockPool(int pageNo) {
        return requireData(ClaudeEastMoneyApiUtil.fetchData(String.format(STOCK_POOL_TEMPLATE, pageNo)), "stock-pool:" + pageNo);
    }

    private String requireData(String rawJson, String jobName) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalStateException("No data returned for " + jobName);
        }
        return rawJson;
    }
}
