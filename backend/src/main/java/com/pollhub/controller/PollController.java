package com.pollhub.controller;

import com.pollhub.dto.*;
import com.pollhub.service.PollService;
import com.pollhub.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;
    private final VoteService voteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PollResponse create(@Valid @RequestBody PollRequest request,
                               @AuthenticationPrincipal Jwt jwt) {
        return pollService.create(request, jwt.getClaimAsString("preferred_username"));
    }

    @GetMapping
    public List<PollResponse> list() {
        return pollService.findAll();
    }

    @GetMapping("/{id}")
    public PollResponse get(@PathVariable Long id) {
        return pollService.findById(id);
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<ResultsResponse> results(@PathVariable Long id,
                                                    @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = jwt.getClaimAsString("preferred_username");
        String creator = pollService.findById(id).createdBy();

        // Le créateur voit toujours les résultats
        // Les autres doivent avoir voté d'abord
        if (!creator.equals(username) && !voteService.hasVotedForPoll(id, username)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Votez d'abord pour voir les résultats.");
        }

        return ResponseEntity.ok(pollService.getResults(id));
    }

    @GetMapping("/my")
    public List<PollResponse> myPolls(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return pollService.findByCreator(username);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        pollService.deleteIfOwner(id, username);
        return ResponseEntity.ok().build();
    }
}