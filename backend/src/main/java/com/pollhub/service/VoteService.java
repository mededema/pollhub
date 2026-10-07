package com.pollhub.service;

import com.pollhub.model.*;
import com.pollhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final PollRepository pollRepository;
    private final StringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;

    public void vote(Long questionId, Long optionId, String username) {
        if (!rateLimitService.isAllowed(username)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Tu votes trop vite.");
        }
        if (voteRepository.existsByQuestionIdAndVoterUsername(questionId, username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tu as deja vote pour cette question.");
        }
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question introuvable"));
        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Option introuvable"));
        if (!option.getQuestion().getId().equals(questionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option invalide");
        }
        Vote vote = Vote.builder().question(question).option(option).voterUsername(username).build();
        voteRepository.save(vote);
        redisTemplate.opsForValue().increment("q:" + questionId + ":opt:" + optionId);
    }

    public boolean hasVotedForPoll(Long pollId, String username) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return poll.getQuestions().stream()
                .anyMatch(q -> voteRepository.existsByQuestionIdAndVoterUsername(q.getId(), username));
    }
}