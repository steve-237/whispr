package com.whispr.backend.controller;

import com.whispr.backend.domain.Advertisement;
import com.whispr.backend.dto.AdvertisementDto;
import com.whispr.backend.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/advertisements")
@RequiredArgsConstructor
public class AdvertisementController {

    private final AdvertisementRepository advertisementRepository;

    @GetMapping
    public ResponseEntity<List<AdvertisementDto>> getActiveAdvertisements() {
        List<AdvertisementDto> ads = advertisementRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ads);
    }

    @PostMapping("/{id}/view")
    @Transactional
    public ResponseEntity<Void> recordView(@PathVariable UUID id) {
        advertisementRepository.findById(id).ifPresent(ad -> {
            ad.setViewsCount(ad.getViewsCount() + 1);
            advertisementRepository.save(ad);
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/click")
    @Transactional
    public ResponseEntity<Void> recordClick(@PathVariable UUID id) {
        advertisementRepository.findById(id).ifPresent(ad -> {
            ad.setClicksCount(ad.getClicksCount() + 1);
            advertisementRepository.save(ad);
        });
        return ResponseEntity.ok().build();
    }

    private AdvertisementDto toDto(Advertisement ad) {
        return new AdvertisementDto(
                ad.getId(),
                ad.getTitle(),
                ad.getDescription(),
                ad.getImageUrl(),
                ad.getTargetUrl(),
                ad.getCtaText(),
                ad.getBadgeText(),
                ad.isActive(),
                ad.getClicksCount(),
                ad.getViewsCount(),
                ad.getCreatedAt()
        );
    }
}
