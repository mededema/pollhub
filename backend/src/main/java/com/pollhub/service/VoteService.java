// backend/src/main/java/com/pollhub/service/VoteService.java
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
    private final PollRepository pollRepository;
    private final OptionRepository optionRepository;
    private final StringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;

    public void vote(Long pollId, Long optionId, String username) {

        // 1. Vérifier le rate limit
        if (!rateLimitService.isAllowed(username)) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Tu votes trop vite. Attends une minute."
            );
        }

        // 2. Vérifier que l'utilisateur n'a pas déjà voté
        if (rateLimitService.hasAlreadyVoted(username, pollId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Tu as déjà voté pour ce sondage."
            );
        }

        // 3. Vérifier que le sondage existe et n'est pas expiré
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sondage introuvable"));

        if (poll.getExpiresAt() != null && poll.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Ce sondage est terminé.");
        }

        // 4. Vérifier que l'option appartient bien à ce sondage
        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Option introuvable"));

        if (!option.getPoll().getId().equals(pollId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette option n'appartient pas à ce sondage");
        }

        // 5. Sauvegarder le vote en base
        Vote vote = Vote.builder()
            .poll(poll)
            .option(option)
            .voterUsername(username)
            .build();
        voteRepository.save(vote);

        // 6. Incrémenter le compteur Redis (pour les résultats en temps réel)
        String redisKey = "poll:" + pollId + ":option:" + optionId;
        redisTemplate.opsForValue().increment(redisKey);

        // 7. Marquer l'utilisateur comme ayant voté
        rateLimitService.markAsVoted(username, pollId);
    }
}