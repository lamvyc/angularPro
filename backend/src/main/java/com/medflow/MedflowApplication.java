package com.medflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MedflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedflowApplication.class, args);
    }
}
