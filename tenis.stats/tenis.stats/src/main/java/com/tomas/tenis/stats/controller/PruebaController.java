package com.tomas.tenis.stats.controller;

import com.tomas.tenis.stats.client.RandomUserClient;
import com.tomas.tenis.stats.dto.UserResponse;
import com.tomas.tenis.stats.soap.CountryInfoService;
import com.tomas.tenis.stats.soap.CountryInfoServiceSoapType;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

import com.tomas.tenis.stats.salesforce.SalesforceAuthService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PruebaController {

    private final SalesforceAuthService authService;
    private final RandomUserClient randomUserClient;

    // ✅ Constructor correcto con ambas dependencias
    public PruebaController(SalesforceAuthService authService, RandomUserClient randomUserClient) {
        this.authService = authService;
        this.randomUserClient = randomUserClient;
    }

    // ✅ Test de conexión
    @GetMapping("/test-salesforce")
    public ResponseEntity<?> probarConexion() {
        try {
            Map<String, Object> auth = authService.getAccessToken();
            return ResponseEntity.ok(auth);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // ✅ Crear Lead en Salesforce
    @PostMapping("/test-crear")
    public ResponseEntity<?> testCrear() {
        try {
            Map<String, Object> auth = authService.getAccessToken();

            String token = (String) auth.get("access_token");
            String instanceUrl = (String) auth.get("instance_url");

            if (!instanceUrl.endsWith("/")) {
                instanceUrl += "/";
            }

            String url = instanceUrl + "services/data/v60.0/sobjects/Partido__c";

            String jsonMatch = """
            {
              "Name": "Final Australia Open",
              "Aces__c": 15,
              "Ganador__c": "Jannik Sinner"
             }""";

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            headers.set("Accept", "application/json");

            HttpEntity<String> request = new HttpEntity<>(jsonMatch, headers);

            ResponseEntity<?> response =
                    restTemplate.postForEntity(url, request, Map.class);

            return ResponseEntity.ok(response.getBody());

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }
    }

    @GetMapping("/test-rest-externo")
    public ResponseEntity<?> testRestExterno() {
        try {
            UserResponse response = randomUserClient.getRandomUser();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/test-soap-pais")
    public ResponseEntity<?> testSoapPais(@RequestParam(defaultValue = "AR") String codigoPais) {
        try {
            CountryInfoService service = new CountryInfoService();
            CountryInfoServiceSoapType port = service.getCountryInfoServiceSoap();

            String capital = port.capitalCity(codigoPais);

            return ResponseEntity.ok("La capital de " + codigoPais + " es: " + capital);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error en SOAP: " + e.getMessage());
        }
    }
}