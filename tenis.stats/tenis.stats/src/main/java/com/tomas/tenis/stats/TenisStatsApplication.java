package com.tomas.tenis.stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class TenisStatsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TenisStatsApplication.class, args);
		System.out.println("¡App arrancada! Intentando conectar con Salesforce...");
	}


}

