package com.finportfolio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 10, max = 128, message = "Sifre en az 10 karakter olmalidir")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
                message = "Sifre en az 1 buyuk harf, 1 kucuk harf, 1 rakam ve 1 ozel karakter icermelidir"
        )
        String password,

        @NotBlank
        @Size(min = 2, max = 100)
        String fullName
) {}