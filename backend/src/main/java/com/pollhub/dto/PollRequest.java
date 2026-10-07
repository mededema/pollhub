package com.pollhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record PollRequest(
        @NotBlank String title,
        String description,
        @NotEmpty List<QuestionRequest> questions,
        LocalDateTime expiresAt
) {}