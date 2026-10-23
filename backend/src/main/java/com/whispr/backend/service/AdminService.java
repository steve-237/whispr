package com.whispr.backend.service;

import com.whispr.backend.domain.Advertisement;
import com.whispr.backend.domain.Role;
import com.whispr.backend.domain.User;
import com.whispr.backend.dto.AdvertisementDto;
import com.whispr.backend.dto.admin.AdminAuditLogDto;
import com.whispr.backend.dto.admin.AdminMessageDto;
import com.whispr.backend.dto.admin.AdminUserDto;
import com.whispr.backend.dto.admin.CreateAdRequest;
import com.whispr.backend.repository.AdvertisementRepository;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.MessageRepository;
import com.whispr.backend.repository.ProfileRepository;
import com.whispr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ProfileRepository profileRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdvertisementRepository advertisementRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalMessages", messageRepository.count());
        stats.put("totalProfiles", profileRepository.count());
        return stats;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> new AdminUserDto(
                        user.getEmail(),
                        user.getPseudo(),
                        user.getRole().name(),
                        user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
                ));
    }

    @Transactional
    public void deleteUser(String pseudo) {
        userRepository.findByPseudo(pseudo).ifPresent(userRepository::delete);
    }

    @Transactional
    public void deleteUsersBulk(List<String> pseudonyms) {
        for (String pseudo : pseudonyms) {
            userRepository.findByPseudo(pseudo).ifPresent(userRepository::delete);
        }
    }

    @Transactional(readOnly = true)
    public Page<AdminMessageDto> getMessages(Pageable pageable) {
        return messageRepository.findAll(pageable)
                .map(msg -> new AdminMessageDto(
                        msg.getId().toString(),
                        msg.getContent(),
                        msg.getLink() != null && msg.getLink().getUser() != null ? msg.getLink().getUser().getPseudo() : "unknown",
                        msg.getToxicityScore() != null && msg.getToxicityScore().doubleValue() > 0.5,
                        msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : ""
                ));
    }

    @Transactional
    public void deleteMessage(UUID id) {
        messageRepository.deleteById(id);
    }

    @Transactional
    public void deleteMessagesBulk(List<UUID> ids) {
        messageRepository.deleteAllById(ids);
    }

    @Transactional
    public void banUser(String pseudo) {
        userRepository.findByPseudo(pseudo).ifPresent(user -> {
            user.setRole(Role.BANNED);
            userRepository.save(user);
        });
    }

    @Transactional
    public void unbanUser(String pseudo) {
        userRepository.findByPseudo(pseudo).ifPresent(user -> {
            user.setRole(Role.USER);
            userRepository.save(user);
        });
    }

    @Transactional
    public Optional<String> resetPassword(String pseudo) {
        return userRepository.findByPseudo(pseudo).map(user -> {
            String newPassword = UUID.randomUUID().toString().substring(0, 8);
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return newPassword;
        });
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLogDto> getAuditLogs(Pageable pageable, boolean isSuperAdmin) {
        return auditLogRepository.findAll(pageable)
                .map(log -> new AdminAuditLogDto(
                        log.getMessage() != null && log.getMessage().getLink() != null && log.getMessage().getLink().getUser() != null
                                ? log.getMessage().getLink().getUser().getPseudo()
                                : "unknown",
                        log.getHashedIp(),
                        isSuperAdmin ? log.getRawIp() : "***.***.***.***",
                        log.getUserAgent(),
                        isSuperAdmin ? log.getCountry() : "Confidentiel",
                        log.getCreatedAt() != null ? log.getCreatedAt().toString() : ""
                ));
    }

    @Transactional(readOnly = true)
    public List<AdvertisementDto> getAdminAds() {
        return advertisementRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toAdDto)
                .toList();
    }

    @Transactional
    public AdvertisementDto createAd(CreateAdRequest req) {
        Advertisement ad = Advertisement.builder()
                .title(req.title())
                .description(req.description())
                .imageUrl(req.imageUrl())
                .targetUrl(req.targetUrl())
                .ctaText(req.ctaText() != null && !req.ctaText().isBlank() ? req.ctaText() : "En savoir plus")
                .badgeText(req.badgeText() != null && !req.badgeText().isBlank() ? req.badgeText() : "Sponsorisé")
                .isActive(true)
                .clicksCount(0)
                .viewsCount(0)
                .build();
        ad = advertisementRepository.save(ad);
        return toAdDto(ad);
    }

    @Transactional
    public void toggleAd(UUID id) {
        advertisementRepository.findById(id).ifPresent(ad -> {
            ad.setActive(!ad.isActive());
            advertisementRepository.save(ad);
        });
    }

    @Transactional
    public void deleteAd(UUID id) {
        advertisementRepository.deleteById(id);
    }

    private AdvertisementDto toAdDto(Advertisement ad) {
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
