package com.pollhub.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

// Conteneurs partages par tous les tests d'integration. Declares comme des beans
// (et non des champs statiques @Container) pour que Spring reutilise le meme contexte
// applicatif, donc les memes conteneurs, d'une classe de test a l'autre au lieu de
// tout redemarrer a chaque fois.
@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:15-alpine");
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    }
}