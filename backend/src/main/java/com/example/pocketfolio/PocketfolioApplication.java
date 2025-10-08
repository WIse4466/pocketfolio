package com.example.pocketfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PocketfolioApplication {

    public static void main(String[] args) {
        SpringApplication.run(PocketfolioApplication.class, args);
    }

}
