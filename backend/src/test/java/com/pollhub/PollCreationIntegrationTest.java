package com.pollhub;

import com.pollhub.dto.PollRequest;
import com.pollhub.dto.QuestionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PollCreationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void creerUnSondageValide_retourne201() throws Exception {
        PollRequest request = new PollRequest(
                "Sondage de test",
                "Une description",
                List.of(new QuestionRequest("Quelle est votre couleur preferee ?", List.of("Bleu", "Rouge"))),
                null
        );

        mockMvc.perform(post("/api/polls")
                        .with(userToken("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Sondage de test"))
                .andExpect(jsonPath("$.questions", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].options", hasSize(2)));
    }

    @Test
    void titreVide_retourne400AvecMessageEtFieldErrors() throws Exception {
        PollRequest request = new PollRequest(
                "",
                null,
                List.of(new QuestionRequest("Une question ?", List.of("A", "B"))),
                null
        );

        mockMvc.perform(post("/api/polls")
                        .with(userToken("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void uneSeuleOption_retourne400() throws Exception {
        PollRequest request = new PollRequest(
                "Sondage incomplet",
                null,
                List.of(new QuestionRequest("Une question ?", List.of("Seule option"))),
                null
        );

        mockMvc.perform(post("/api/polls")
                        .with(userToken("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors", aMapWithSize(greaterThan(0))));
    }

    @Test
    void optionsNulles_retourne400() throws Exception {
        PollRequest request = new PollRequest(
                "Sondage avec options nulles",
                null,
                List.of(new QuestionRequest("Une question ?", null)),
                null
        );

        mockMvc.perform(post("/api/polls")
                        .with(userToken("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors", aMapWithSize(greaterThan(0))));
    }

    @Test
    void questionSansTexte_retourne400() throws Exception {
        PollRequest request = new PollRequest(
                "Sondage avec question vide",
                null,
                List.of(new QuestionRequest("", List.of("A", "B"))),
                null
        );

        mockMvc.perform(post("/api/polls")
                        .with(userToken("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors", aMapWithSize(greaterThan(0))));
    }
}
