package com.jeybell.sheetmusic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SheetmusicApplication {

    public static void main(String[] args) {
        SpringApplication.run(SheetmusicApplication.class, args);
    }
}
