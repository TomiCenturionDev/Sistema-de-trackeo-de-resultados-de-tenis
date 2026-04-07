package com.tomas.tenis.stats.salesforce.service;

import com.tomas.tenis.stats.model.Partido;
import com.tomas.tenis.stats.salesforce.SalesforceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class SalesforceDataService {

    @Autowired
    private SalesforceAuthService authService;

    public String enviarPartidoASalesforce(Partido partido, String externalId) {
        Map<String, Object> data = new HashMap<>();

        data.put("Name", "Partido: " + partido.getTorneo());
        data.put("Torneo__c", partido.getTorneo());
        data.put("Ciudad__c", partido.getCiudad());
        data.put("Pais__c", partido.getPais());
        data.put("Superficie__c", partido.getSuperficie().name());
        data.put("Fase__c", partido.getFase().name());
        data.put("Estado__c", partido.getEstado().name());
        data.put("Jugador_1__c", partido.getJugador1().getNombre());
        data.put("Jugador_2__c", partido.getJugador2().getNombre());
        data.put("Resultado__c", partido.getResultado());
        data.put("Ganador__c", partido.getGanador());
        data.put("Fecha__c", partido.getFecha().toString());

        if (partido.getCategoria() != null) {
            data.put("Puntos__c", partido.getCategoria().getPuntos());
        }

        System.out.println("🔍 External ID recibido: " + externalId);

        return realizarUpsert("Partido__c", "Id_Local__c", externalId, data);
    }

    public String realizarUpsert(String sObject, String externalIdField, String externalIdValue, Map<String, Object> data) {
        Map<String, Object> auth = authService.getAccessToken();
        String token = (String) auth.get("access_token");
        String dinamicInstanceUrl = (String) auth.get("instance_url");

        String url = dinamicInstanceUrl + "/services/data/v58.0/sobjects/" + sObject + "/" + externalIdField + "/" + externalIdValue + "?_HttpMethod=PATCH";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("X-HTTP-Method-Override", "PATCH");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(data, headers);
        restTemplate.postForEntity(url, request, Map.class);

        return "☁️ ¡UPSERT EXITOSO! Smart ID: " + externalIdValue;
    }
}