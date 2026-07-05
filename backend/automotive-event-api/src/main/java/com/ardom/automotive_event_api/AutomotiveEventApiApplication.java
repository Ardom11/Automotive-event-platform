package com.ardom.automotive_event_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AutomotiveEventApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutomotiveEventApiApplication.class, args);
    }

}
