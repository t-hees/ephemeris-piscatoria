package com.tadeo.fish_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FishProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(FishProjectApplication.class, args);
    }
}
