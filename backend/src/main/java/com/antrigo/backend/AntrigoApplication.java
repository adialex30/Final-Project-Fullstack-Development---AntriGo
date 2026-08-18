package com.antrigo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AntrigoApplication {
    public static void main(String[] args) {
        SpringApplication.run(AntrigoApplication.class, args);
    }
}
