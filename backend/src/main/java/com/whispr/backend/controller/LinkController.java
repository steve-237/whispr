package com.whispr.backend.controller;

import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Profile;
import com.whispr.backend.dto.LinkDto;
import com.whispr.backend.service.LinkService;
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

    @GetMapping("/{slug}")
    public ResponseEntity<LinkDto> getLinkInfo(@PathVariable String slug) {
        return linkService.getLinkBySlug(slug)
                .map(link -> {
                    Profile profile = profileRepository.findByUserId(link.getUser().getId())
                            .orElseThrow(() -> new RuntimeException("Profile not found"));
                    return ResponseEntity.ok(new LinkDto(
                            link.getId(),
                            link.getSlug(),
                            link.getIsActive(),
                            profile.getBio(),
                            profile.getAvatarUrl(),
                            profile.getThemeId(),
                            profile.getDailyQuestion()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
