package com.whispr.backend.controller;

import com.whispr.backend.dto.*;
import com.whispr.backend.service.PromptService;
import com.whispr.backend.util.ClientMetadata;
import com.whispr.backend.util.ClientMetadataResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;
    private final ClientMetadataResolver clientMetadataResolver;

    @GetMapping("/my")
    public ResponseEntity<List<PromptDto>> getMyPrompts(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(promptService.getUserPrompts(email));
    }

    @PostMapping
    public ResponseEntity<PromptDto> createPrompt(
            @RequestBody CreatePromptRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(promptService.createPrompt(email, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<PromptDto> togglePromptStatus(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean active,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(promptService.togglePromptStatus(email, id, active));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(
            @PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        promptService.deletePrompt(email, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/{slug}")
    public ResponseEntity<PromptDto> getPublicActivePrompt(@PathVariable String slug) {
        return promptService.getActivePromptForSlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/public/{slug}/respond")
    public ResponseEntity<PromptResponseResultDto> respondToPrompt(
            @PathVariable String slug,
            @RequestBody RespondPromptRequest request,
            HttpServletRequest httpRequest) {
        ClientMetadata metadata = clientMetadataResolver.resolve(httpRequest);
        PromptResponseResultDto result = promptService.respondToPrompt(slug, request, metadata != null ? metadata.hashedIp() : "anon");
        return ResponseEntity.ok(result);
    }
}
