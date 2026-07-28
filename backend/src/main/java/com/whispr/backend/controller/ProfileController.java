package com.whispr.backend.controller;

import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Profile;
import com.whispr.backend.domain.User;
import com.whispr.backend.dto.ProfileUpdateRequest;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.repository.ProfileRepository;
import com.whispr.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final ProfileRepository profileRepository;
    private final LinkRepository linkRepository;

    @PutMapping("/my")
    @Transactional
    public ResponseEntity<Void> updateMyProfile(
            @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.dailyQuestion() != null) {
            profile.setDailyQuestion(request.dailyQuestion());
        }
        if (request.themeId() != null) {
            profile.setThemeId(request.themeId());
        }
        profileRepository.save(profile);

        if (request.isActive() != null) {
            Link link = linkRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Link not found"));
            link.setIsActive(request.isActive());
            linkRepository.save(link);
        }

        return ResponseEntity.ok().build();
    }
}
