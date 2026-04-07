package com.tomas.tenis.stats;

import com.tomas.tenis.stats.salesforce.SalesforceAuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@EnableFeignClients
@SpringBootApplication
@EnableScheduling
public class TenisStatsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TenisStatsApplication.class, args);
		System.out.println("¡App arrancada! Intentando conectar con Salesforce...");
	}

	// ✅ CommandLineRunner para probar JWT y obtener access_token
	@Bean
	public CommandLineRunner testSalesforceToken(SalesforceAuthService authService) {
		return args -> {
			org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TenisStatsApplication.class);

			log.info("🔹 Generando JWT y solicitando access_token a Salesforce...");

			try {
				Map<String, Object> tokenResponse = authService.getAccessToken();
				log.info("✅ Access Token recibido: {}", tokenResponse);

			} catch (Exception e) {
				log.error("❌ Error obteniendo token de Salesforce", e);
			}
		};
	}
}