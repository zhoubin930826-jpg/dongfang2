package com.example.houduan;

import com.example.houduan.utils.ClaudeEastMoneyApiUtil;
import com.example.houduan.utils.EastMoneyApiUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HouduanApplication {

    public static void main(String[] args) {
        // 初始化 EastMoney API 工具，获取 Cookie
        EastMoneyApiUtil.init();
        SpringApplication.run(HouduanApplication.class, args);
    }

}
