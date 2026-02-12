package com.example.houduan.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * 东方财富API工具类 - 增强防反爬版本
 * 主要优化点：
 * 1. User-Agent池随机化
 * 2. 请求频率控制（令牌桶算法）
 * 3. 智能重试机制
 * 4. Cookie池管理
 * 5. 请求指纹随机化
 * 6. 代理支持
 * 7. 完善的错误处理
 */
public class ClaudeEastMoneyApiUtil {

    // ==================== 配置常量 ====================

    private static final String DEFAULT_COOKIE = "qgqp_b_id=94d44a0f08221ddfb2428bdd8bfe7b11; websitepoptg_api_time=1770711215544; st_nvi=Ik9rC3AnoCdAMCgHrgKXxa96b; nid18=06026b47b71743c68fc1927735443bbe; nid18_create_time=1770711216117; gviem=BRkNT6EyXAqgHguxnVYawb5ce; gviem_create_time=1770711216117; st_pvi=04493722350580; st_sp=2026-02-10%2016%3A13%3A35; st_inirUrl=https%3A%2F%2Fwww.baidu.com%2Flink";
    private static final long COOKIE_REFRESH_INTERVAL = 10 * 60 * 1000; // 10分钟
    private static final int MAX_RETRY_TIMES = 3; // 最大重试次数
    private static final long BASE_DELAY = 2000; // 基础延迟2秒
    private static final long RANDOM_DELAY = 3000; // 随机延迟0-3秒
    
    // ==================== User-Agent 池 ====================
    
    private static final String[] USER_AGENTS = {
        // Chrome Windows
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        
        // Chrome macOS
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        
        // Firefox Windows
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0",
        
        // Firefox macOS
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:123.0) Gecko/20100101 Firefox/123.0",
        
        // Edge
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0",
        
        // Safari
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15"
    };
    
    // ==================== Accept-Language 池 ====================
    
    private static final String[] ACCEPT_LANGUAGES = {
        "zh-CN,zh;q=0.9,en;q=0.8",
        "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2",
        "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
        "zh-CN,zh;q=0.8,en;q=0.5",
        "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7"
    };
    
    // ==================== 状态管理 ====================
    
    private static final Map<String, String> cookiePool = new ConcurrentHashMap<>();
    private static String currentCookie = DEFAULT_COOKIE;
    private static long lastCookieUpdateTime = 0;
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    
    // 令牌桶 - 频率控制 (每秒最多2个请求)
    private static long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL = 500; // 最小请求间隔500ms
    
    // ==================== 代理配置（可选） ====================
    
    private static boolean useProxy = false;
    private static List<ProxyConfig> proxyList = new ArrayList<>();
    private static int currentProxyIndex = 0;
    
    static class ProxyConfig {
        String host;
        int port;
        
        ProxyConfig(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
    
    // ==================== 核心方法 ====================
    
    /**
     * 获取数据 - 主入口方法
     * @param urlString 请求URL
     * @return 响应内容
     */
    public static String fetchData(String urlString) {
        return fetchDataWithRetry(urlString, 0);
    }
    
    /**
     * 带重试的数据获取
     * @param urlString 请求URL
     * @param retryCount 当前重试次数
     * @return 响应内容
     */
    private static String fetchDataWithRetry(String urlString, int retryCount) {
        // 频率控制
        rateLimitControl();
        
        // 刷新Cookie
        refreshCookieIfNeeded();
        
        // 请求前随机延迟
        randomDelay();
        
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = createConnection(url);
            
            // 设置随机化请求头
            setRandomHeaders(conn, urlString);
            
            int responseCode = conn.getResponseCode();
            
            // 根据响应码处理
            if (responseCode == 200) {
                String content = readResponse(conn);
                conn.disconnect();
                logSuccess(urlString, retryCount);
                return content;
                
            } else if (responseCode == 429 || responseCode == 503) {
                // 请求过快或服务不可用
                conn.disconnect();
                return handleRateLimit(urlString, retryCount, responseCode);
                
            } else if (responseCode == 403 || responseCode == 401) {
                // 可能被封禁，刷新Cookie
                conn.disconnect();
                return handleForbidden(urlString, retryCount, responseCode);
                
            } else {
                conn.disconnect();
                return handleOtherError(urlString, retryCount, responseCode);
            }
            
        } catch (Exception e) {
            return handleException(urlString, retryCount, e);
        }
    }
    
    /**
     * 创建HTTP连接
     */
    private static HttpURLConnection createConnection(URL url) throws Exception {
        HttpURLConnection conn;
        
        if (useProxy && !proxyList.isEmpty()) {
            Proxy proxy = getNextProxy();
            conn = (HttpURLConnection) url.openConnection(proxy);
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }
        
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setInstanceFollowRedirects(true);
        
        return conn;
    }
    
    /**
     * 设置随机化请求头
     */
    private static void setRandomHeaders(HttpURLConnection conn, String urlString) {
        String userAgent = getRandomUserAgent();
        String acceptLanguage = getRandomAcceptLanguage();
        
        // 基础请求头
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        conn.setRequestProperty("Accept-Language", acceptLanguage);
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Cookie", currentCookie);
        conn.setRequestProperty("Connection", "keep-alive");
        
        // Referer - 根据URL智能设置
        String referer = getReferer(urlString);
        if (referer != null) {
            conn.setRequestProperty("Referer", referer);
        }
        
        // 根据 User-Agent 类型设置不同的头
        if (userAgent.contains("Chrome") || userAgent.contains("Edge")) {
            conn.setRequestProperty("sec-ch-ua", getSecChUa(userAgent));
            conn.setRequestProperty("sec-ch-ua-mobile", "?0");
            conn.setRequestProperty("sec-ch-ua-platform", getSecChUaPlatform(userAgent));
            // 注意：Sec-Fetch-* 头最好不要手动设置，或根据实际情况设置
            // conn.setRequestProperty("Sec-Fetch-Dest", "document");
            // conn.setRequestProperty("Sec-Fetch-Mode", "navigate");
            // conn.setRequestProperty("Sec-Fetch-Site", "none");
            // conn.setRequestProperty("Sec-Fetch-User", "?1");
        }
        
        // 随机添加一些可选头
        if (ThreadLocalRandom.current().nextBoolean()) {
            conn.setRequestProperty("Cache-Control", "max-age=0");
        }
        
        if (ThreadLocalRandom.current().nextBoolean()) {
            conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
        }
        
        // DNT (Do Not Track)
        if (ThreadLocalRandom.current().nextInt(100) < 30) {
            conn.setRequestProperty("DNT", "1");
        }
    }
    
    /**
     * 读取响应内容
     */
    private static String readResponse(HttpURLConnection conn) throws Exception {
        String encoding = conn.getContentEncoding();
        
        BufferedReader reader;
        if ("gzip".equalsIgnoreCase(encoding)) {
            reader = new BufferedReader(
                new InputStreamReader(
                    new GZIPInputStream(conn.getInputStream()),
                    StandardCharsets.UTF_8
                )
            );
        } else {
            reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );
        }
        
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        reader.close();
        
        return content.toString();
    }
    
    /**
     * 处理频率限制响应
     */
    private static String handleRateLimit(String urlString, int retryCount, int responseCode) {
        if (retryCount >= MAX_RETRY_TIMES) {
            System.err.println("达到最大重试次数，请求失败: " + urlString + " (响应码: " + responseCode + ")");
            return null;
        }
        
        long waitTime = (long) Math.pow(2, retryCount) * 5000; // 指数退避: 5s, 10s, 20s
        System.out.println("请求过快 (响应码: " + responseCode + ")，等待 " + waitTime + "ms 后重试...");
        
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return fetchDataWithRetry(urlString, retryCount + 1);
    }
    
    /**
     * 处理403/401响应
     */
    private static String handleForbidden(String urlString, int retryCount, int responseCode) {
        if (retryCount >= MAX_RETRY_TIMES) {
            System.err.println("达到最大重试次数，可能被封禁: " + urlString + " (响应码: " + responseCode + ")");
            return null;
        }
        
        System.out.println("可能被封禁 (响应码: " + responseCode + ")，刷新Cookie后重试...");
        
        // 强制刷新Cookie
        fetchCookies();
        
        // 等待一段时间
        try {
            Thread.sleep(3000 * (retryCount + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return fetchDataWithRetry(urlString, retryCount + 1);
    }
    
    /**
     * 处理其他错误响应
     */
    private static String handleOtherError(String urlString, int retryCount, int responseCode) {
        if (retryCount >= MAX_RETRY_TIMES) {
            System.err.println("达到最大重试次数，请求失败: " + urlString + " (响应码: " + responseCode + ")");
            return null;
        }
        
        System.err.println("请求失败 (响应码: " + responseCode + ")，重试中... (" + (retryCount + 1) + "/" + MAX_RETRY_TIMES + ")");
        
        try {
            Thread.sleep(2000 * (retryCount + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return fetchDataWithRetry(urlString, retryCount + 1);
    }
    
    /**
     * 处理异常
     */
    private static String handleException(String urlString, int retryCount, Exception e) {
        if (retryCount >= MAX_RETRY_TIMES) {
            System.err.println("达到最大重试次数，请求异常: " + urlString);
            e.printStackTrace();
            return null;
        }
        
        System.err.println("请求异常，重试中... (" + (retryCount + 1) + "/" + MAX_RETRY_TIMES + "): " + e.getMessage());
        
        try {
            Thread.sleep(2000 * (retryCount + 1));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        
        return fetchDataWithRetry(urlString, retryCount + 1);
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 频率控制
     */
    private static synchronized void rateLimitControl() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        
        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL) {
            long sleepTime = MIN_REQUEST_INTERVAL - timeSinceLastRequest;
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        lastRequestTime = System.currentTimeMillis();
    }
    
    /**
     * 随机延迟
     */
    private static void randomDelay() {
        try {
            long delay = BASE_DELAY + ThreadLocalRandom.current().nextLong(RANDOM_DELAY);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 检查并刷新Cookie
     */
    private static void refreshCookieIfNeeded() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastCookieUpdateTime > COOKIE_REFRESH_INTERVAL) {
            fetchCookies();
            lastCookieUpdateTime = currentTime;
        }
    }
    
    /**
     * 获取Cookie
     */
    private static void fetchCookies() {
        try {
            URL url = new URL("https://quote.eastmoney.com/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", getRandomUserAgent());
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                Map<String, List<String>> headerFields = conn.getHeaderFields();
                List<String> setCookieHeaders = headerFields.get("Set-Cookie");
                
                if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String cookie : setCookieHeaders) {
                        String[] parts = cookie.split(";");
                        if (parts.length > 0) {
                            sb.append(parts[0]).append("; ");
                        }
                    }
                    currentCookie = sb.toString();
                    System.out.println("Cookie 已更新: " + currentCookie.substring(0, Math.min(100, currentCookie.length())) + "...");
                } else {
                    System.out.println("未获取到新Cookie，继续使用旧Cookie");
                }
            }
            
            conn.disconnect();
            
        } catch (Exception e) {
            System.err.println("Cookie 刷新失败，继续使用旧 Cookie: " + e.getMessage());
        }
    }
    
    /**
     * 获取随机 User-Agent
     */
    private static String getRandomUserAgent() {
        return USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.length)];
    }
    
    /**
     * 获取随机 Accept-Language
     */
    private static String getRandomAcceptLanguage() {
        return ACCEPT_LANGUAGES[ThreadLocalRandom.current().nextInt(ACCEPT_LANGUAGES.length)];
    }
    
    /**
     * 智能获取 Referer
     */
    private static String getReferer(String urlString) {
        if (urlString.contains("eastmoney.com")) {
            return "https://quote.eastmoney.com/";
        }
        // 可以根据不同域名设置不同的 referer
        return null;
    }
    
    /**
     * 生成 sec-ch-ua 头
     */
    private static String getSecChUa(String userAgent) {
        if (userAgent.contains("Chrome/120")) {
            return "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"";
        } else if (userAgent.contains("Chrome/119")) {
            return "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"119\", \"Google Chrome\";v=\"119\"";
        } else if (userAgent.contains("Edge/120")) {
            return "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Microsoft Edge\";v=\"120\"";
        }
        return "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\"";
    }
    
    /**
     * 生成 sec-ch-ua-platform 头
     */
    private static String getSecChUaPlatform(String userAgent) {
        if (userAgent.contains("Windows")) {
            return "\"Windows\"";
        } else if (userAgent.contains("Macintosh")) {
            return "\"macOS\"";
        }
        return "\"Windows\"";
    }
    
    /**
     * 记录成功日志
     */
    private static void logSuccess(String urlString, int retryCount) {
        int count = requestCounter.incrementAndGet();
        if (retryCount > 0) {
            System.out.println("请求成功 [重试" + retryCount + "次] (总计: " + count + "): " + urlString);
        } else {
            // 每10次请求打印一次日志，避免刷屏
            if (count % 10 == 0) {
                System.out.println("请求成功 (总计: " + count + ")");
            }
        }
    }
    
    // ==================== 代理相关方法 ====================
    
    /**
     * 启用代理
     */
    public static void enableProxy(boolean enable) {
        useProxy = enable;
        System.out.println("代理模式: " + (enable ? "启用" : "禁用"));
    }
    
    /**
     * 添加代理
     */
    public static void addProxy(String host, int port) {
        proxyList.add(new ProxyConfig(host, port));
        System.out.println("添加代理: " + host + ":" + port);
    }
    
    /**
     * 获取下一个代理（轮询）
     */
    private static synchronized Proxy getNextProxy() {
        if (proxyList.isEmpty()) {
            return Proxy.NO_PROXY;
        }
        
        ProxyConfig config = proxyList.get(currentProxyIndex);
        currentProxyIndex = (currentProxyIndex + 1) % proxyList.size();
        
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.host, config.port));
    }
    
    /**
     * 清空代理列表
     */
    public static void clearProxies() {
        proxyList.clear();
        currentProxyIndex = 0;
        System.out.println("代理列表已清空");
    }
    
    // ==================== 初始化方法 ====================
    
    /**
     * 初始化工具类
     * 建议在应用启动时调用
     */
    public static void init() {
        System.out.println("==================== EastMoneyApiUtil 初始化 ====================");
        System.out.println("初始化 Cookie...");
        fetchCookies();
        lastCookieUpdateTime = System.currentTimeMillis();
        System.out.println("User-Agent 池大小: " + USER_AGENTS.length);
        System.out.println("最小请求间隔: " + MIN_REQUEST_INTERVAL + "ms");
        System.out.println("Cookie 刷新间隔: " + COOKIE_REFRESH_INTERVAL + "ms");
        System.out.println("最大重试次数: " + MAX_RETRY_TIMES);
        System.out.println("==============================================================");
    }
    
    /**
     * 获取统计信息
     */
    public static String getStats() {
        return String.format(
            "统计信息 - 总请求数: %d, 当前Cookie长度: %d, 上次Cookie更新: %d分钟前, 代理: %s",
            requestCounter.get(),
            currentCookie.length(),
            (System.currentTimeMillis() - lastCookieUpdateTime) / 60000,
            useProxy ? "启用(" + proxyList.size() + "个)" : "禁用"
        );
    }
    
    /**
     * 重置统计
     */
    public static void resetStats() {
        requestCounter.set(0);
        System.out.println("统计信息已重置");
    }
}
