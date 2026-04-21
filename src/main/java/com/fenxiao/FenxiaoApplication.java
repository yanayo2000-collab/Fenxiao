package com.fenxiao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FenxiaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FenxiaoApplication.class, args);
    }
}
