package com.evolutionbot.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "integration.evolution")
public record EvolutionApiProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @NotBlank String instance,
        @NotBlank String webhookToken
) {
}
