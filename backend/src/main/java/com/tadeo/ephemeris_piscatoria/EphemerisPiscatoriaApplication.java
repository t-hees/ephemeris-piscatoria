package com.tadeo.ephemeris_piscatoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EphemerisPiscatoriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EphemerisPiscatoriaApplication.class, args);
    }
}
