package br.com.amorEmMechas_Formulario.api.para.formulario.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.dev-token.enabled=true")
@ActiveProfiles("test")
class JwtServletFilterIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("JWT demo autentica a requisicao passando pelo container servlet")
    void tokenDemoAutenticaForaDoMockMvc() throws Exception {
        ResponseEntity<String> issued = restTemplate.postForEntity(
                baseUrl() + "/auth/dev-token",
                Map.of("username", "demo.atendente", "role", "ROLE_ATENDENTE"),
                String.class);
        JsonNode tokens = objectMapper.readTree(issued.getBody());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.get("accessToken").asText());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/madrinhas",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertEquals(200, response.getStatusCode().value());

        ResponseEntity<String> refreshed = restTemplate.postForEntity(
            baseUrl() + "/auth/refresh",
            Map.of("refreshToken", tokens.get("refreshToken").asText()),
            String.class);
        JsonNode refreshedToken = objectMapper.readTree(refreshed.getBody());
        headers.setBearerAuth(refreshedToken.get("accessToken").asText());

        ResponseEntity<String> refreshedResponse = restTemplate.exchange(
            baseUrl() + "/madrinhas",
            org.springframework.http.HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

        assertEquals(200, refreshedResponse.getStatusCode().value());

        assertEquals(401, restTemplate.getForEntity(baseUrl() + "/madrinhas", String.class)
            .getStatusCode().value());

        ResponseEntity<String> userTokenResponse = restTemplate.postForEntity(
            baseUrl() + "/auth/dev-token",
            Map.of("username", "demo.usuario", "role", "ROLE_USER"),
            String.class);
        String userToken = objectMapper.readTree(userTokenResponse.getBody())
            .get("accessToken").asText();
        headers.setBearerAuth(userToken);
        ResponseEntity<String> forbiddenResponse = restTemplate.exchange(
            baseUrl() + "/madrinhas",
            org.springframework.http.HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

        assertEquals(403, forbiddenResponse.getStatusCode().value());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}