package com.evolutionbot.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank String phoneNumber,
        @NotBlank String text
) {
}
