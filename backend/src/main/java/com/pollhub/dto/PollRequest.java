package com.pollhub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record PollRequest(
        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 120, message = "Le titre ne peut pas dépasser 120 caractères.")
        String title,

        @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères.")
        String description,

        // @Valid sur l'element de la liste : sans ca, les contraintes de QuestionRequest
        // ne sont jamais verifiees (c'etait le bug initial)
        @NotNull(message = "Le sondage doit contenir au moins une question.")
        @Size(min = 1, max = 20, message = "Un sondage contient entre 1 et 20 questions.")
        List<@Valid QuestionRequest> questions,

        @Future(message = "La date d'expiration doit être dans le futur.")
        LocalDateTime expiresAt
) {}