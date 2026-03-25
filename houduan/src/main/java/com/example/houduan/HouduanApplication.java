package com.example.houduan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HouduanApplication {

    public static void main(String[] args) {
        // 初始化 EastMoney API 工具，获取 Cookie
        SpringApplication.run(HouduanApplication.class, args);
    }

}
