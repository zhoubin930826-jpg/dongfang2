package com.example.houduan.config;

import com.example.houduan.utils.ClaudeEastMoneyApiUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;

/**
 * 爬虫配置类 - 应用启动时自动配置
 */
@Configuration
public class CrawlerConfig {
    
    // ==================== 方案 1：从配置文件读取（推荐） ====================
    
    @Value("${crawler.proxy.enabled:false}")
    private boolean proxyEnabled;
    
    @Value("${crawler.proxy.servers:}")
    private String proxyServers;  // 格式: host1:port1,host2:port2,host3:port3

    @Value("${crawler.request.min-interval-ms:900}")
    private long minRequestIntervalMs;

    @Value("${crawler.request.base-delay-ms:150}")
    private long baseDelayMs;

    @Value("${crawler.request.random-delay-ms:350}")
    private long randomDelayMs;

    @Value("${crawler.cookie.refresh-interval-ms:600000}")
    private long cookieRefreshIntervalMs;

    @Value("${crawler.retry.max-attempts:4}")
    private int maxRetryTimes;

    @Value("${crawler.cooldown.rate-limit-ms:30000}")
    private long rateLimitCooldownMs;

    @Value("${crawler.cooldown.forbidden-ms:60000}")
    private long forbiddenCooldownMs;

    @Value("${crawler.cooldown.failure-threshold:6}")
    private int consecutiveFailureThreshold;

    @Value("${crawler.cooldown.failure-ms:45000}")
    private long failureCooldownMs;

    @Value("${crawler.proxy.cooldown-ms:180000}")
    private long proxyCooldownMs;

    @Value("${crawler.proxy.fail-open:true}")
    private boolean proxyFailOpen;
    
    @PostConstruct
    public void init() {
        System.out.println("==================== 初始化爬虫配置 ====================");

        ClaudeEastMoneyApiUtil.configure(
            minRequestIntervalMs,
            baseDelayMs,
            randomDelayMs,
            cookieRefreshIntervalMs,
            maxRetryTimes,
            rateLimitCooldownMs,
            forbiddenCooldownMs,
            consecutiveFailureThreshold,
            failureCooldownMs,
            proxyCooldownMs,
            proxyFailOpen
        );
        
        // 初始化工具类
        ClaudeEastMoneyApiUtil.init();
        
        // 配置代理
        if (proxyEnabled && proxyServers != null && !proxyServers.isEmpty()) {
            String[] servers = proxyServers.split(",");
            for (String server : servers) {
                String[] parts = server.trim().split(":");
                if (parts.length == 2) {
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    ClaudeEastMoneyApiUtil.addProxy(host, port);
                    System.out.println("已添加代理: " + host + ":" + port);
                }
            }
            ClaudeEastMoneyApiUtil.enableProxy(true);
            System.out.println("代理模式已启用");
        } else {
            System.out.println("代理模式未启用，使用本地IP");
        }
        
        System.out.println("=======================================================");
    }
}
