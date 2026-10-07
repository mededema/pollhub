package com.pollhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record PollRequest(
        @NotBlank String question,
        @Size(min = 2, max = 10) List<@NotBlank String> options,
        LocalDateTime expiresAt
) {}