package com.pollhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuestionRequest(
        @NotBlank String text,
        @Size(min = 2, max = 10) List<@NotBlank String> options
) {}