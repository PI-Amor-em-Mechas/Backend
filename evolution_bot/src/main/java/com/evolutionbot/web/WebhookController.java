package com.evolutionbot.web;

import com.evolutionbot.config.EvolutionApiProperties;
import com.evolutionbot.service.WebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/webhooks/evolution")
public class WebhookController {

    private final WebhookService webhookService;
    private final EvolutionApiProperties properties;

    public WebhookController(WebhookService webhookService, EvolutionApiProperties properties) {
        this.webhookService = webhookService;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody JsonNode payload,
                                        @RequestHeader(name = "x-webhook-token", required = false) String token) {
        if (token == null || !token.equals(properties.webhookToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook token");
        }

        webhookService.process(payload);
        return ResponseEntity.accepted().build();
    }
}
