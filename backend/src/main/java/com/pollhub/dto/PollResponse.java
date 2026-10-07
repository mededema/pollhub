package com.pollhub.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PollResponse(
        Long id,
        String title,
        String description,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        String createdBy,
        List<QuestionResponse> questions
) {}