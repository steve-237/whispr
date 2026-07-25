package com.whispr.backend.controller;

import com.whispr.backend.domain.User;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.MessageRepository;
import com.whispr.backend.repository.ProfileRepository;
import com.whispr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ProfileRepository profileRepository;
    private final AuditLogRepository auditLogRepository;

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

    @GetMapping("/audit-logs")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<AuditLogDto>> getAuditLogs() {
        List<AuditLogDto> logs = auditLogRepository.findAll().stream()
                .map(log -> new AuditLogDto(
                        log.getMessage().getLink().getUser().getPseudo(),
                        log.getHashedIp(),
                        log.getCountry(),
                        log.getCreatedAt().toString()
                )).collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }
    
    public record UserDto(String email, String pseudo, String role, String createdAt) {}
    public record AuditLogDto(String targetUser, String hashedIp, String country, String createdAt) {}
}
