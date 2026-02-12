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
    
    @PostConstruct
    public void init() {
        System.out.println("==================== 初始化爬虫配置 ====================");
        
        // 初始化工具类
        ClaudeEastMoneyApiUtil.init();
        
//        // 配置代理
//        if (proxyEnabled && proxyServers != null && !proxyServers.isEmpty()) {
//            String[] servers = proxyServers.split(",");
//            for (String server : servers) {
//                String[] parts = server.trim().split(":");
//                if (parts.length == 2) {
//                    String host = parts[0];
//                    int port = Integer.parseInt(parts[1]);
//                    ClaudeEastMoneyApiUtil.addProxy(host, port);
//                    System.out.println("已添加代理: " + host + ":" + port);
//                }
//            }
//            ClaudeEastMoneyApiUtil.enableProxy(true);
//            System.out.println("代理模式已启用");
//        } else {
//            System.out.println("代理模式未启用，使用本地IP");
//        }
        
        System.out.println("=======================================================");
    }
}
