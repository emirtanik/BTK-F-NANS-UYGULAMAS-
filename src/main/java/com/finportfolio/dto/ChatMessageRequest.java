package com.finportfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank
        @Size(min = 1, max = 1000, message = "Mesaj 1-1000 karakter olmali")
        String message
) {}