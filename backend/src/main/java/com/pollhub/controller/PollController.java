package com.pollhub.controller;

import com.pollhub.dto.*;
import com.pollhub.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

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
    public ResultsResponse results(@PathVariable Long id) {
        return pollService.getResults(id);
    }
}