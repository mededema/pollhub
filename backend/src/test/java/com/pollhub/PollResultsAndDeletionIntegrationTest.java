package com.pollhub;

import com.pollhub.dto.PollResponse;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PollResultsAndDeletionIntegrationTest extends AbstractIntegrationTest {

    @Test
    void resultats_sansJeton_retourne401() throws Exception {
        PollResponse poll = createPoll("claire", "Sondage resultats sans jeton", null);

        mockMvc.perform(get("/api/polls/{id}/results", poll.id()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void resultats_sansAvoirVote_retourne403() throws Exception {
        PollResponse poll = createPoll("claire", "Sondage resultats sans vote", null);

        mockMvc.perform(get("/api/polls/{id}/results", poll.id())
                        .with(userToken("daniel")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void resultats_apresVote_retourne200() throws Exception {
        PollResponse poll = createPoll("claire", "Sondage resultats apres vote", null);
        Long questionId = poll.questions().get(0).id();
        Long optionId = poll.questions().get(0).options().get(0).id();

        mockMvc.perform(post("/api/votes/questions/{qid}/options/{oid}", questionId, optionId)
                        .with(userToken("daniel")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/polls/{id}/results", poll.id())
                        .with(userToken("daniel")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(1));
    }

    @Test
    void resultats_parLeCreateur_retourne200() throws Exception {
        PollResponse poll = createPoll("claire", "Sondage resultats createur", null);

        mockMvc.perform(get("/api/polls/{id}/results", poll.id())
                        .with(userToken("claire")))
                .andExpect(status().isOk());
    }

    @Test
    void suppression_parUnAutreUtilisateur_retourne403() throws Exception {
        PollResponse poll = createPoll("claire", "Sondage protege", null);

        mockMvc.perform(delete("/api/polls/{id}", poll.id())
                        .with(userToken("daniel")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(get("/api/polls/{id}", poll.id()))
                .andExpect(status().isOk());
    }

    @Test
    void suppression_parLeProprietaireAvecDesVotes_retourne200() throws Exception {
        PollResponse poll = createPoll("claire", "Sondage a supprimer", null);
        Long questionId = poll.questions().get(0).id();
        Long optionId = poll.questions().get(0).options().get(0).id();

        mockMvc.perform(post("/api/votes/questions/{qid}/options/{oid}", questionId, optionId)
                        .with(userToken("daniel")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/polls/{id}", poll.id())
                        .with(userToken("claire")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/polls/{id}", poll.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void mesSondages_sansJeton_retourne401() throws Exception {
        mockMvc.perform(get("/api/polls/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
