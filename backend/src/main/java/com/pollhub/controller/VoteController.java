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

    @PostMapping("/questions/{questionId}/options/{optionId}")
    public ResponseEntity<Void> vote(
        @PathVariable Long questionId,
        @PathVariable Long optionId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        String username = jwt.getClaimAsString("preferred_username");
        voteService.vote(questionId, optionId, username);
        return ResponseEntity.ok().build();
    }
}