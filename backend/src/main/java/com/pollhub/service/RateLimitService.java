// backend/src/main/java/com/pollhub/service/RateLimitService.java
package com.pollhub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Value("${pollhub.rate-limit.max-votes-per-minute}")
    private int maxVotesPerMinute;

    /**
     * Vérifie si l'utilisateur a dépassé la limite de votes
     * Clé Redis : "rate:vote:USERNAME" → expire après 1 minute
     */
    public boolean isAllowed(String username) {
        String key = "rate:vote:" + username;

        Long count = redisTemplate.opsForValue().increment(key);

        // Première fois qu'on voit cet utilisateur dans la fenêtre → on set l'expiration
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        return count <= maxVotesPerMinute;
    }

    /**
     * Vérifie si un utilisateur a déjà voté pour CE sondage spécifique
     * Un utilisateur = un vote par sondage, point.
     */
    public boolean hasAlreadyVoted(String username, Long pollId) {
        String key = "voted:" + username + ":poll:" + pollId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void markAsVoted(String username, Long pollId) {
        String key = "voted:" + username + ":poll:" + pollId;
        // On garde cette info 30 jours
        redisTemplate.opsForValue().set(key, "1", Duration.ofDays(30));
    }
}