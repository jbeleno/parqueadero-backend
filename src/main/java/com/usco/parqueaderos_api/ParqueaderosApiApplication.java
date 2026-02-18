package com.usco.parqueaderos_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ParqueaderosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParqueaderosApiApplication.class, args);
	}

}
