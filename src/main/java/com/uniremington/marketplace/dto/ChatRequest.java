package com.uniremington.marketplace.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Escribe un mensaje antes de enviarlo.")
        @MaxChatInput
        String mensaje
) {
}
