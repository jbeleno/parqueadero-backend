package com.usco.parqueaderos_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableAsync esta en AsyncConfig.java con configuracion del ThreadPool
@SpringBootApplication
@EnableScheduling
public class ParqueaderosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParqueaderosApiApplication.class, args);
	}

}
