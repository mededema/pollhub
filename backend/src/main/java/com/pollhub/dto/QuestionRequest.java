package com.pollhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuestionRequest(
        @NotBlank(message = "Le texte de la question est obligatoire.")
        @Size(max = 200, message = "Le texte de la question ne peut pas dépasser 200 caractères.")
        String text,

        // @NotNull est indispensable : @Size seul considere une liste null comme valide,
        // ce qui provoquait une NullPointerException plus loin dans le service
        @NotNull(message = "Les options sont obligatoires.")
        @Size(min = 2, max = 10, message = "Une question doit avoir entre 2 et 10 options.")
        List<
            @NotBlank(message = "Une option ne peut pas être vide.")
            @Size(max = 100, message = "Une option ne peut pas dépasser 100 caractères.")
            String
        > options
) {}