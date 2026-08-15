package com.whispr.backend.controller;

import com.whispr.backend.domain.User;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.MessageRepository;
import com.whispr.backend.repository.ProfileRepository;
import com.whispr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ProfileRepository profileRepository;
    private final AuditLogRepository auditLogRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalMessages", messageRepository.count());
        stats.put("totalProfiles", profileRepository.count());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(user -> new UserDto(user.getEmail(), user.getPseudo(), user.getRole().name(), user.getCreatedAt().toString()))
                .toList();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{pseudo}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable String pseudo) {
        userRepository.findByPseudo(pseudo).ifPresent(userRepository::delete);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/bulk")
    @Transactional
    public ResponseEntity<Void> deleteUsersBulk(@RequestBody List<String> pseudonyms) {
        for (String pseudo : pseudonyms) {
            userRepository.findByPseudo(pseudo).ifPresent(userRepository::delete);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/messages")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MessageDto>> getMessages() {
        List<MessageDto> messages = messageRepository.findAll().stream()
                .map(msg -> new MessageDto(
                        msg.getId().toString(),
                        msg.getContent(),
                        msg.getLink() != null && msg.getLink().getUser() != null ? msg.getLink().getUser().getPseudo() : "unknown",
                        msg.getToxicityScore() != null && msg.getToxicityScore().doubleValue() > 0.5,
                        msg.getCreatedAt().toString()
                )).collect(Collectors.toList());
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/messages/{id}")
    @Transactional
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID id) {
        messageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages/bulk")
    @Transactional
    public ResponseEntity<Void> deleteMessagesBulk(@RequestBody List<UUID> ids) {
        messageRepository.deleteAllById(ids);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{pseudo}/ban")
    @Transactional
    public ResponseEntity<Void> banUser(@PathVariable String pseudo) {
        userRepository.findByPseudo(pseudo).ifPresent(user -> {
            user.setRole(com.whispr.backend.domain.Role.BANNED);
            userRepository.save(user);
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{pseudo}/unban")
    @Transactional
    public ResponseEntity<Void> unbanUser(@PathVariable String pseudo) {
        userRepository.findByPseudo(pseudo).ifPresent(user -> {
            user.setRole(com.whispr.backend.domain.Role.USER);
            userRepository.save(user);
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{pseudo}/reset-password")
    @Transactional
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable String pseudo) {
        return userRepository.findByPseudo(pseudo).map(user -> {
            String newPassword = UUID.randomUUID().toString().substring(0, 8);
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("newPassword", newPassword));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/audit-logs")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AuditLogDto>> getAuditLogs() {
        List<AuditLogDto> logs = auditLogRepository.findAll().stream()
                .map(log -> new AuditLogDto(
                        log.getMessage() != null && log.getMessage().getLink() != null && log.getMessage().getLink().getUser() != null ? log.getMessage().getLink().getUser().getPseudo() : "unknown",
                        log.getHashedIp(),
                        log.getRawIp(),
                        log.getUserAgent(),
                        log.getCountry(),
                        log.getCreatedAt().toString()
                )).collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }
    
    public record UserDto(String email, String pseudo, String role, String createdAt) {}
    public record MessageDto(String id, String content, String targetUser, Boolean isToxic, String createdAt) {}
    public record AuditLogDto(String targetUser, String hashedIp, String rawIp, String userAgent, String country, String createdAt) {}
}
