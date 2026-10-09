package com.pollhub.config;

import com.pollhub.exception.RestAccessDeniedHandler;
import com.pollhub.exception.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    // liste separee par des virgules, injectee via une variable d'environnement en k8s
    @Value("${pollhub.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Ces deux routes sont couvertes par le pattern public /api/polls/**
                // ci-dessous : elles doivent donc etre declarees AVANT lui, sinon
                // un appel sans jeton tombe dans le controleur avec un JWT nul (bug corrige ici).
                .requestMatchers(HttpMethod.GET, "/api/polls/my").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/polls/*/results").authenticated()
                // Lecture publique des sondages
                .requestMatchers(HttpMethod.GET, "/api/polls/**").permitAll()
                // Actuator health : accessible sans authentification (sondes k8s)
                .requestMatchers("/actuator/health/**").permitAll()
                // Creer un sondage ou voter : il faut etre connecte
                .requestMatchers(HttpMethod.POST, "/api/polls/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/votes/**").authenticated()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
                // jeton absent, expiré ou invalide : même réponse JSON que le reste de l'API
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}