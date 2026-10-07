package com.pollhub.dto;

import java.util.List;

public record ResultsResponse(
        Long pollId,
        String title,
        long totalVotes,
        List<QuestionResult> questions
) {}