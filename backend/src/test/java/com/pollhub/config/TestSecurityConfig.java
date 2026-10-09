package com.pollhub.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

// Les tests n'ont pas de Keycloak : SecurityMockMvcRequestPostProcessors.jwt() injecte
// directement une authentification dans le contexte de securite, sans jamais appeler
// ce decodeur. Il n'existe que pour permettre au contexte Spring Security de demarrer
// (un bean JwtDecoder est requis par la configuration du resource server).
@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        return token -> {
            throw new UnsupportedOperationException("JwtDecoder factice : non utilise dans les tests");
        };
    }
}