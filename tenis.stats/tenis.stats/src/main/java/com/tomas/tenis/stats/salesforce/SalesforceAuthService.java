package com.tomas.tenis.stats.salesforce;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Service
public class SalesforceAuthService {

    @Value("${salesforce.client.id}")
    private String clientId;

    @Value("${salesforce.username}")
    private String username;

    @Value("${salesforce.login.url}")
    private String loginUrl; // Solo https://login.salesforce.com

    private static final Logger log = LoggerFactory.getLogger(SalesforceAuthService.class);

    public Map<String, Object> getAccessToken() {
        try {
            // ✅ 1. Cargar private key desde resources
            ClassPathResource resource = new ClassPathResource("server.key");
            String keyContent = new String(Files.readAllBytes(resource.getFile().toPath()));
            String privateKeyPEM = keyContent
                    .replaceAll("-----BEGIN (RSA )?PRIVATE KEY-----", "")
                    .replaceAll("-----END (RSA )?PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
            log.info("✅ Private key cargada correctamente.");

            // ✅ 2. Generar JWT
            Algorithm algorithm = Algorithm.RSA256(null, privateKey);
            Date expiration = new Date(System.currentTimeMillis() + 300_000); // 5 min
            String jwtToken = JWT.create()
                    .withIssuer(clientId)
                    .withSubject(username)
                    .withAudience(loginUrl)
                    .withExpiresAt(expiration)
                    .sign(algorithm);
            log.info("✅ JWT generado correctamente.");

            // 🔹 3. Decodificar JWT para debug (sin verificar firma)
            DecodedJWT decodedJWT = JWT.decode(jwtToken);
            log.info("JWT payload:\n{}", decodedJWT.getPayload()); // base64url codificado
            log.info("JWT claims:\niss={}, sub={}, aud={}, exp={}",
                    decodedJWT.getIssuer(),
                    decodedJWT.getSubject(),
                    decodedJWT.getAudience().get(0),
                    decodedJWT.getExpiresAt());

            // ✅ 4. Preparar POST a Salesforce
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            params.add("assertion", jwtToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            String tokenEndpoint = loginUrl + "/services/oauth2/token";

            log.info("Enviando request a Salesforce: {}", tokenEndpoint);

            // ✅ 5. Ejecutar POST y manejar respuesta
            ResponseEntity<?> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();

            if (response.getStatusCode() == HttpStatus.OK && body != null) {
                log.info("✅ Access token recibido: {}", body.get("access_token"));
                return body;
            }

            throw new RuntimeException("Error obteniendo token: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("❌ Error en la autenticación JWT", e);
            throw new RuntimeException("Error en la autenticación JWT: " + e.getMessage(), e);
        }
    }
}