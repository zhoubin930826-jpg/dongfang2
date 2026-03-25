package graph;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * TODO
 *
 * @author: ZhouBin
 * @date: 2026/2/11 15:41
 */
public class Test20260211_v2 {

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
            System.out.println("Cookie 需要刷新，正在获取...");
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
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                Map<String, List<String>> headerFields = conn.getHeaderFields();
                List<String> setCookieHeaders = headerFields.get("Set-Cookie");

                if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String cookie : setCookieHeaders) {
                        sb.append(cookie.split(";")[0]).append("; ");
                    }
                    cookieString = sb.toString();
                    System.out.println("Cookie 已更新");
                }
            }

            conn.disconnect();

        } catch (Exception e) {
            System.err.println("Cookie 刷新失败，继续使用旧 Cookie");
        }
    }

    // 请求数据（带自动 Cookie 刷新）
    public static String fetchData() {
        // 自动检查并刷新 Cookie
        refreshCookieIfNeeded();

        try {
            String urlString = "https://push2.eastmoney.com/api/qt/clist/get?pn=1&pz=2000&po=1&np=1&fltt=2&invt=2&wbp2u=%7C0%7C0%7C0%7Cweb&fid=f3&fs=m:90+t:2+f:%2150&fields=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f26,f22,f33,f11,f62,f128,f136,f115,f152,f124,f107,f104,f105,f106,f140,f141,f207,f208,f209,f222";

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Referer", "https://quote.eastmoney.com/");
            System.out.println("Cookie: " + cookieString);
            conn.setRequestProperty("Cookie", cookieString);

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
            }

            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String[] args) {
        // 首次运行时初始化 Cookie
        System.out.println("初始化 Cookie...");
        fetchCookies();
        lastCookieUpdateTime = System.currentTimeMillis();

        // 请求数据（会自动管理 Cookie）
        System.out.println("\n请求数据...");
        String result = fetchData();
        System.out.println(result);

        // 模拟多次请求
        // for (int i = 0; i < 5; i++) {
        //     String data = fetchData();
        //     Thread.sleep(2000);
        // }
    }
}
