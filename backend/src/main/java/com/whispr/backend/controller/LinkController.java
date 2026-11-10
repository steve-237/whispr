package com.whispr.backend.controller;

import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Profile;
import com.whispr.backend.dto.LinkDto;
import com.whispr.backend.dto.PromptDto;
import com.whispr.backend.service.LinkService;
import com.whispr.backend.service.PromptService;
import com.whispr.backend.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;
    private final ProfileRepository profileRepository;
    private final PromptService promptService;

    @GetMapping("/{slug}")
    public ResponseEntity<LinkDto> getLinkInfo(@PathVariable String slug) {
        return linkService.getLinkBySlug(slug)
                .map(link -> {
                    Profile profile = profileRepository.findByUserId(link.getUser().getId())
                            .orElseThrow(() -> new RuntimeException("Profile not found"));
                    PromptDto activePrompt = promptService != null ? promptService.getActivePromptForSlug(link.getSlug()).orElse(null) : null;
                    return ResponseEntity.ok(new LinkDto(
                            link.getId(),
                            link.getSlug(),
                            link.getIsActive(),
                            profile.getBio(),
                            profile.getAvatarUrl(),
                            profile.getThemeId(),
                            profile.getDailyQuestion(),
                            activePrompt
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
