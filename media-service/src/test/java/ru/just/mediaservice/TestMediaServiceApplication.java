package ru.just.mediaservice;

import org.springframework.boot.SpringApplication;

public class TestMediaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(MediaServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
