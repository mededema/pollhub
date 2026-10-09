package com.pollhub.service;

import com.pollhub.dto.*;
import com.pollhub.model.*;
import com.pollhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final VoteRepository voteRepository;
    private final StringRedisTemplate redisTemplate;

    public PollResponse create(PollRequest request, String username) {
        Poll poll = Poll.builder()
                .title(request.title())
                .description(request.description())
                .expiresAt(request.expiresAt())
                .createdBy(username)
                .build();

        AtomicInteger order = new AtomicInteger(0);
        List<Question> questions = request.questions().stream()
                .map(qr -> {
                    Question q = Question.builder()
                            .text(qr.text())
                            .sortOrder(order.getAndIncrement())
                            .poll(poll)
                            .build();
                    List<Option> options = qr.options().stream()
                            .map(label -> Option.builder().label(label).question(q).build())
                            .toList();
                    q.setOptions(options);
                    return q;
                }).toList();

        poll.setQuestions(questions);
        return toResponse(pollRepository.save(poll));
    }

    // open-in-view est desactive : sans transaction, le parcours de poll.getQuestions()
    // dans toResponse() echouerait des que la collection est chargee en LAZY
    @Transactional(readOnly = true)
    public List<PollResponse> findAll() {
        return pollRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PollResponse findById(Long id) {
        return toResponse(getPoll(id));
    }

    @Transactional(readOnly = true)
    public ResultsResponse getResults(Long pollId) {
        Poll poll = getPoll(pollId);

        List<QuestionResult> qResults = poll.getQuestions().stream()
                .map(q -> {
                    List<OptionResult> oResults = q.getOptions().stream()
                            .map(o -> new OptionResult(o.getId(), o.getLabel(), readCounter(q.getId(), o.getId())))
                            .toList();
                    long total = oResults.stream().mapToLong(OptionResult::votes).sum();
                    return new QuestionResult(q.getId(), q.getText(), total, oResults);
                }).toList();

        long grandTotal = qResults.stream().mapToLong(QuestionResult::totalVotes).sum();
        return new ResultsResponse(poll.getId(), poll.getTitle(), grandTotal, qResults);
    }

    private long readCounter(Long questionId, Long optionId) {
        String key = "q:" + questionId + ":opt:" + optionId;
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
        List<QuestionResponse> qs = p.getQuestions().stream()
                .map(q -> new QuestionResponse(
                        q.getId(),
                        q.getText(),
                        q.getSortOrder(),
                        q.getOptions().stream()
                                .map(o -> new OptionResponse(o.getId(), o.getLabel()))
                                .toList()
                )).toList();
        return new PollResponse(p.getId(), p.getTitle(), p.getDescription(),
                p.getCreatedAt(), p.getExpiresAt(), p.getCreatedBy(), qs);
    }

    @Transactional(readOnly = true)
    public List<PollResponse> findByCreator(String username) {
        return pollRepository.findByCreatedBy(username).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void deleteIfOwner(Long id, String username) {
        Poll poll = getPoll(id);
        if (!poll.getCreatedBy().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas le propriétaire de ce sondage.");
        }

        // Clés Redis des compteurs de ce sondage
        List<String> keys = new ArrayList<>();
        for (Question q : poll.getQuestions()) {
            for (Option o : q.getOptions()) {
                keys.add("q:" + q.getId() + ":opt:" + o.getId());
            }
        }

        voteRepository.deleteByQuestionPollId(id); // 1. supprimer les votes
        pollRepository.delete(poll);               // 2. puis le sondage
        redisTemplate.delete(keys);                // 3. nettoyer Redis
    }
}