package com.pollhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollhub.config.ContainersConfig;
import com.pollhub.config.TestSecurityConfig;
import com.pollhub.dto.PollRequest;
import com.pollhub.dto.PollResponse;
import com.pollhub.dto.QuestionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Base commune a tous les tests d'integration : demarre Postgres et Redis via Testcontainers,
// et fournit un jeton JWT simule sans dependre d'un vrai serveur Keycloak.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ContainersConfig.class, TestSecurityConfig.class})
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // simule un utilisateur authentifie par Keycloak : le filtre de securite recoit
    // directement cette authentification, sans jamais passer par un JwtDecoder reel
    protected RequestPostProcessor userToken(String username) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("preferred_username", username));
    }

    // cree un sondage a une question / deux options, utilise comme donnee de depart
    // par la plupart des tests de vote et de resultats
    protected PollResponse createPoll(String creatorUsername, String title, LocalDateTime expiresAt) throws Exception {
        PollRequest request = new PollRequest(
                title,
                "Description de test",
                List.of(new QuestionRequest("Question de test ?", List.of("Option A", "Option B"))),
                expiresAt
        );

        String json = mockMvc.perform(post("/api/polls")
                        .with(userToken(creatorUsername))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(json, PollResponse.class);
    }
}
