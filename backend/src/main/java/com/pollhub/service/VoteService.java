package com.pollhub.service;

import com.pollhub.model.*;
import com.pollhub.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import java.util.Collections;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final PollRepository pollRepository;
    private final StringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;


        // n'incrémente que si le compteur existe déjà : s'il a été perdu (redémarrage de Redis),
    // PollService.readCounter() le reconstruit depuis la base, vote compris
    private static final RedisScript<Long> INCREMENT_IF_EXISTS = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[1]) == 1 then return redis.call('INCR', KEYS[1]) end return -1",
            Long.class
    );

    @Transactional
    public void vote(Long questionId, Long optionId, String username) {
        if (!rateLimitService.isAllowed(username)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Vous votez trop vite.");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question introuvable"));
        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Option introuvable"));

        if (!option.getQuestion().getId().equals(questionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette option n'appartient pas à cette question.");
        }

        LocalDateTime expiresAt = question.getPoll().getExpiresAt();
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Ce sondage est expiré.");
        }

        // controle rapide pour un message agreable ; la vraie protection contre
        // les doublons reste la contrainte unique en base, verifiee juste en dessous
        if (voteRepository.existsByQuestionIdAndVoterUsername(questionId, username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà voté pour cette question.");
        }

        Vote vote = Vote.builder().question(question).option(option).voterUsername(username).build();
        try {
            // flush immediat : la violation de contrainte doit remonter ici,
            // pas au moment du commit ou elle echapperait au try/catch
            voteRepository.saveAndFlush(vote);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà voté pour cette question.");
        }

        incrementCounterAfterCommit(questionId, optionId);
    }

    // le compteur Redis n'est touche qu'une fois la transaction definitivement validee :
    // si elle est annulee (contrainte violee, erreur quelconque), aucun compteur fantome n'est incremente
    private void incrementCounterAfterCommit(Long questionId, Long optionId) {
        String key = "q:" + questionId + ":opt:" + optionId;

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            incrementCounter(key);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                incrementCounter(key);
            }
        });
    }

    private void incrementCounter(String key) {
        try {
             redisTemplate.execute(INCREMENT_IF_EXISTS, Collections.singletonList(key));
        } catch (Exception e) {
            // si Redis est indisponible, on supprime la cle plutot que de la laisser fausse :
            // PollService.readCounter() la recalculera depuis la base au prochain GET des resultats
            log.error("Echec de l'incrementation du compteur Redis {}", key, e);
            try {
                redisTemplate.delete(key);
            } catch (Exception deleteFailure) {
                log.error("Echec de la suppression de la cle Redis {} apres erreur d'incrementation", key, deleteFailure);
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean hasVotedForPoll(Long pollId, String username) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return poll.getQuestions().stream()
                .anyMatch(q -> voteRepository.existsByQuestionIdAndVoterUsername(q.getId(), username));
    }
}