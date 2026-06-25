package com.entain.sportevents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class SportEventsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportEventsApplication.class, args);
    }
}
