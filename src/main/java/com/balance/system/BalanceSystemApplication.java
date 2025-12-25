package com.balance.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableCaching // 启用缓存
@EnableRetry  // 启用重试机制
public class BalanceSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BalanceSystemApplication.class, args);
    }

}