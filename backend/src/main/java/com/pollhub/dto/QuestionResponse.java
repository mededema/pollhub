package com.pollhub.dto;

import java.util.List;

public record QuestionResponse(
        Long id,
        String text,
        int sortOrder,
        List<OptionResponse> options
) {}