package com.fenxiao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@EnableScheduling
@SpringBootApplication
public class FenxiaoApplication {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(FenxiaoApplication.class, args);
    }
}
