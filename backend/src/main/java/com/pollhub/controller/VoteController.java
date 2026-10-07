// backend/src/main/java/com/pollhub/controller/VoteController.java
package com.pollhub.controller;

import com.pollhub.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/polls/{pollId}/options/{optionId}")
    public ResponseEntity<Void> vote(
        @PathVariable Long pollId,
        @PathVariable Long optionId,
        @AuthenticationPrincipal Jwt jwt  // Spring injecte automatiquement le token décodé
    ) {
        // On récupère le username depuis le token Keycloak
        String username = jwt.getClaimAsString("preferred_username");

        voteService.vote(pollId, optionId, username);

        return ResponseEntity.ok().build();
    }
}