package com.evolutionbot.service;

import com.evolutionbot.config.EvolutionApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class EvolutionApiClient {

    private final RestClient restClient;
    private final EvolutionApiProperties properties;
    private final ObjectMapper objectMapper;

    public EvolutionApiClient(RestClient restClient,
                              EvolutionApiProperties properties,
                              ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String sendText(String number, String text) {
        JsonNode response = restClient.post()
                .uri(properties.baseUrl() + "/message/sendText/" + properties.instance())
                .contentType(MediaType.APPLICATION_JSON)
                .header("apikey", properties.apiKey())
                .body(Map.of("number", number, "text", text))
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            return null;
        }

        return response.path("key").path("id").asText(null);
    }

    public String extractText(JsonNode payload) {
        if (payload == null) {
            return null;
        }

        JsonNode message = payload.path("data").path("message");
        if (message.has("conversation")) {
            return message.path("conversation").asText();
        }

        if (message.has("extendedTextMessage")) {
            return message.path("extendedTextMessage").path("text").asText(null);
        }

        return null;
    }

    public JsonNode toJsonNode(Object obj) {
        return objectMapper.valueToTree(obj);
    }
}
