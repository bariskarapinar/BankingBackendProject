package com.fintech.wallet.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.fintech.wallet.api",
        "com.fintech.wallet.application",
        "com.fintech.wallet.infrastructure",
        "com.fintech.wallet.domain"
})
public class DigitalWalletPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(DigitalWalletPlatformApplication.class, args);
    }
}
