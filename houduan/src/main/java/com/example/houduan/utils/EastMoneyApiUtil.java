package com.example.houduan.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class EastMoneyApiUtil {

    // 默认 Cookie（作为备选）
    private static final String DEFAULT_COOKIE = "qgqp_b_id=94d44a0f08221ddfb2428bdd8bfe7b11; websitepoptg_api_time=1770711215544; st_nvi=Ik9rC3AnoCdAMCgHrgKXxa96b; nid18=06026b47b71743c68fc1927735443bbe; nid18_create_time=1770711216117; gviem=BRkNT6EyXAqgHguxnVYawb5ce; gviem_create_time=1770711216117; st_pvi=04493722350580; st_sp=2026-02-10%2016%3A13%3A35; st_inirUrl=https%3A%2F%2Fwww.baidu.com%2Flink";

    private static String cookieString = DEFAULT_COOKIE;
    private static long lastCookieUpdateTime = 0;
    private static final long COOKIE_REFRESH_INTERVAL = 10 * 60 * 1000; // 10分钟刷新一次

    // 检查并刷新 Cookie
    private static void refreshCookieIfNeeded() {
        long currentTime = System.currentTimeMillis();

        // 如果距离上次更新超过10分钟，则刷新
        if (currentTime - lastCookieUpdateTime > COOKIE_REFRESH_INTERVAL) {
            fetchCookies();
            lastCookieUpdateTime = currentTime;
        }
    }

    // 获取 Cookie
    private static void fetchCookies() {
        try {
            URL url = new URL("https://quote.eastmoney.com/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                Map<String, List<String>> headerFields = conn.getHeaderFields();
                List<String> setCookieHeaders = headerFields.get("Set-Cookie");

                if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String cookie : setCookieHeaders) {
                        if (cookie.split(";").length > 0) {
                            sb.append(cookie.split("; ")[0]).append("; ");
                        }
                    }
                    cookieString = sb.toString();
                    System.out.println("Cookie 已更新: " + cookieString);
                }
            }

            conn.disconnect();

        } catch (Exception e) {
            System.err.println("Cookie 刷新失败，继续使用旧 Cookie: " + e.getMessage());
        }
    }

    // 通用请求数据方法（带自动 Cookie 刷新）
    public static String fetchData(String urlString) {
        // 自动检查并刷新 Cookie
        refreshCookieIfNeeded();

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            // 增加更多浏览器特征请求头
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0");
            conn.setRequestProperty("Referer", "https://quote.eastmoney.com/");
            conn.setRequestProperty("Cookie", cookieString);
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
            conn.setRequestProperty("Sec-Fetch-Dest", "document");
            conn.setRequestProperty("Sec-Fetch-Mode", "navigate");
            conn.setRequestProperty("Sec-Fetch-Site", "same-origin");
            conn.setRequestProperty("Sec-Fetch-User", "?1");
            conn.setRequestProperty("Cache-Control", "max-age=0");

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
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
                conn.disconnect();

                return content.toString();
            } else {
                System.err.println("请求失败，响应码: " + responseCode);
            }

            conn.disconnect();

        } catch (Exception e) {
            System.err.println("请求数据失败: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // 初始化方法，在应用启动时调用
    public static void init() {
        System.out.println("初始化 Cookie...");
        fetchCookies();
        lastCookieUpdateTime = System.currentTimeMillis();
    }
}