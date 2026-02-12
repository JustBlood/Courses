package ru.just.monolithmvp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MonolithMvpApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonolithMvpApplication.class, args);
    }
}
