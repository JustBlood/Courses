package ru.just.personalaccountservice;

import org.springframework.boot.SpringApplication;

public class TestPersonalAccountServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(PersonalAccountServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
