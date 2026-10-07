package com.pollhub.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PollResponse(
        Long id,
        String question,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        String createdBy,
        List<OptionResponse> options
) {}