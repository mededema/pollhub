package com.pollhub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Value("${pollhub.rate-limit.max-votes-per-minute}")
    private int maxVotesPerMinute;

    // Increment et pose de l'expiration dans un seul script Lua, execute atomiquement par Redis.
    // Avant, les deux appels etaient separes cote client : si le process s'arretait entre les deux,
    // la cle restait incrementee mais sans TTL, pour toujours.
    private static final RedisScript<Long> INCREMENT_AND_EXPIRE = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) " +
            "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "return count",
            Long.class
    );

    /**
     * Vérifie si l'utilisateur a dépassé la limite de votes
     * Clé Redis : "rate:vote:USERNAME" → expire après 1 minute
     */
    public boolean isAllowed(String username) {
        String key = "rate:vote:" + username;

        Long count = redisTemplate.execute(
                INCREMENT_AND_EXPIRE,
                Collections.singletonList(key),
                String.valueOf(Duration.ofMinutes(1).getSeconds())
        );

        return count != null && count <= maxVotesPerMinute;
    }

    /**
     * Vérifie si un utilisateur a déjà voté pour CE sondage spécifique
     * Un utilisateur = un vote par sondage, point.
     */
    // public boolean hasAlreadyVoted(String username, Long pollId) {
    //     String key = "voted:" + username + ":poll:" + pollId;
    //     return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    // }

    // public void markAsVoted(String username, Long pollId) {
    //     String key = "voted:" + username + ":poll:" + pollId;
    //     // On garde cette info 30 jours
    //     redisTemplate.opsForValue().set(key, "1", Duration.ofDays(30));
    // }
}