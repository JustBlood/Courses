package ru.just.personalaccountservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class PersonalAccountServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalAccountServiceApplication.class, args);
	}

}
