package com.example.BotAI.DTOs;

public record ConfirmationResponse(
        String message,
        String actionId,
        String confirmUrl,
        String cancelUrl
) {
}
