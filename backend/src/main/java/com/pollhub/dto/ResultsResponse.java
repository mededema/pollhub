package com.pollhub.dto;

import java.util.List;

public record ResultsResponse(Long pollId, String question, long totalVotes, List<OptionResult> results) {}