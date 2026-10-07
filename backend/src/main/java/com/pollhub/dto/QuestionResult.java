package com.pollhub.dto;

import java.util.List;

public record QuestionResult(
        Long questionId,
        String text,
        long totalVotes,
        List<OptionResult> options
) {}