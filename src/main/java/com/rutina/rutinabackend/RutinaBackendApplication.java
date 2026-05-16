package com.rutina.rutinabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RutinaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RutinaBackendApplication.class, args);
    }

}
