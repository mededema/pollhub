package com.pollhub.service;

import com.pollhub.dto.*;
import com.pollhub.model.Option;
import com.pollhub.model.Poll;
import com.pollhub.repository.PollRepository;
import com.pollhub.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final VoteRepository voteRepository;
    private final StringRedisTemplate redisTemplate;

    public PollResponse create(PollRequest request, String username) {
        Poll poll = Poll.builder()
                .question(request.question())
                .expiresAt(request.expiresAt())
                .createdBy(username)
                .build();

        List<Option> options = request.options().stream()
                .map(label -> Option.builder().label(label).poll(poll).build())
                .toList();
        poll.setOptions(options);

        return toResponse(pollRepository.save(poll));
    }

    public List<PollResponse> findAll() {
        return pollRepository.findAll().stream().map(this::toResponse).toList();
    }

    public PollResponse findById(Long id) {
        return toResponse(getPoll(id));
    }

    /** Résultats lus dans Redis (temps réel), avec repli sur Postgres si la clé n'existe pas. */
    public ResultsResponse getResults(Long pollId) {
        Poll poll = getPoll(pollId);

        List<OptionResult> results = poll.getOptions().stream()
                .map(o -> new OptionResult(o.getId(), o.getLabel(), readCounter(pollId, o.getId())))
                .toList();

        long total = results.stream().mapToLong(OptionResult::votes).sum();
        return new ResultsResponse(poll.getId(), poll.getQuestion(), total, results);
    }

    private long readCounter(Long pollId, Long optionId) {
        String key = "poll:" + pollId + ":option:" + optionId;
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return Long.parseLong(value);
        }
        long fromDb = voteRepository.countByOptionId(optionId);
        redisTemplate.opsForValue().set(key, String.valueOf(fromDb));
        return fromDb;
    }

    private Poll getPoll(Long id) {
        return pollRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sondage introuvable"));
    }

    private PollResponse toResponse(Poll p) {
        List<OptionResponse> opts = p.getOptions().stream()
                .map(o -> new OptionResponse(o.getId(), o.getLabel()))
                .toList();
        return new PollResponse(p.getId(), p.getQuestion(), p.getCreatedAt(),
                p.getExpiresAt(), p.getCreatedBy(), opts);
    }
}