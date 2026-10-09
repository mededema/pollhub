package com.pollhub;

import com.pollhub.dto.PollResponse;
import com.pollhub.repository.VoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VoteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void voteValide_retourne200() throws Exception {
        PollResponse poll = createPoll("eve", "Sondage vote simple", null);
        Long questionId = poll.questions().get(0).id();
        Long optionId = poll.questions().get(0).options().get(0).id();

        mockMvc.perform(post("/api/votes/questions/{qid}/options/{oid}", questionId, optionId)
                        .with(userToken("felix")))
                .andExpect(status().isOk());
    }

    @Test
    void doubleVote_retourne409() throws Exception {
        PollResponse poll = createPoll("eve", "Sondage double vote", null);
        Long questionId = poll.questions().get(0).id();
        Long optionId = poll.questions().get(0).options().get(0).id();

        mockMvc.perform(post("/api/votes/questions/{qid}/options/{oid}", questionId, optionId)
                        .with(userToken("gina")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/votes/questions/{qid}/options/{oid}", questionId, optionId)
                        .with(userToken("gina")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void voteSurSondageExpire_retourne410() throws Exception {
        PollResponse poll = createPoll("eve", "Sondage qui expire", null);
        Long questionId = poll.questions().get(0).id();
        Long optionId = poll.questions().get(0).options().get(0).id();

        // la creation refuse une date d'expiration passee (@Future) : on simule
        // l'expiration directement en base, comme si le temps s'etait ecoule
        jdbcTemplate.update("UPDATE polls SET expires_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)), poll.id());

        mockMvc.perform(post("/api/votes/questions/{qid}/options/{oid}", questionId, optionId)
                        .with(userToken("hugo")))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void dixVotesSimultanes_memeUtilisateur_unSeulEstEnregistre() throws Exception {
        PollResponse poll = createPoll("eve", "Sondage concurrence", null);
        Long questionId = poll.questions().get(0).id();
        Long optionId = poll.questions().get(0).options().get(0).id();
        RequestPostProcessor token = userToken("ivan");

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        AtomicInteger otherCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int statusCode = mockMvc.perform(post("/api/votes/questions/{qid}/options/{oid}", questionId, optionId)
                                    .with(token))
                            .andReturn().getResponse().getStatus();
                    if (statusCode == 200) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 409) {
                        conflictCount.incrementAndGet();
                    } else {
                        otherCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    otherCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // tous les threads sont prets, on les libere en meme temps pour maximiser la collision
        startLatch.countDown();
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threads - 1);
        assertThat(otherCount.get()).isZero();

        long votesInDb = voteRepository.countByOptionId(optionId);
        assertThat(votesInDb).isEqualTo(1);

        // le total affiche doit lui aussi rester a 1 : compteur Redis coherent avec la base
        // le total affiché doit lui aussi rester à 1 : compteur Redis cohérent avec la base
        mockMvc.perform(get("/api/polls/{id}/results", poll.id()).with(userToken("eve")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(1));
    }
}
