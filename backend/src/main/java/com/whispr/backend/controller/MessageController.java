package com.whispr.backend.controller;

import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.Message;
import com.whispr.backend.dto.MessageDto;
import com.whispr.backend.dto.MessageSendRequest;
import com.whispr.backend.service.MessageService;
import com.whispr.backend.service.UserService;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.util.DeviceUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final LinkRepository linkRepository;
    private final AuditLogRepository auditLogRepository;

    @PostMapping("/send/{slug}")
    public ResponseEntity<Void> sendMessage(
            @PathVariable String slug,
            @RequestBody MessageSendRequest request,
            HttpServletRequest httpRequest) {
        
        // Extraction de l'IP réelle avec support proxies (Cloudflare, Nginx, X-Forwarded-For)
        String ip = httpRequest.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isBlank()) ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = httpRequest.getRemoteAddr();

        String hashedIp = Integer.toHexString(ip != null ? ip.hashCode() : 0);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        // Extraction de la géolocalisation ou simulation réaliste en environnement local/démo
        String country = httpRequest.getHeader("CF-IPCountry");
        if (country == null || country.isBlank() || "XX".equals(country)) {
            country = httpRequest.getHeader("X-Country");
        }
        if (country == null || country.isBlank() || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || (ip != null && (ip.startsWith("192.168.") || ip.startsWith("10.")))) {
            String[] demoLocations = {
                "Paris, France 🇫🇷",
                "Lyon, France 🇫🇷",
                "Marseille, France 🇫🇷",
                "Bordeaux, France 🇫🇷",
                "Montréal, Canada 🇨🇦",
                "Genève, Suisse 🇨🇭",
                "Bruxelles, Belgique 🇧🇪",
                "Casablanca, Maroc 🇲🇦",
                "Dakar, Sénégal 🇸🇳",
                "Abidjan, Côte d'Ivoire 🇨🇮"
            };
            int idx = Math.abs((userAgent != null ? userAgent.hashCode() : (int) System.currentTimeMillis()) % demoLocations.length);
            country = demoLocations[idx];
        }

        messageService.sendMessage(slug, request.content(), hashedIp, userAgent, country);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<MessageDto>> getInbox(org.springframework.security.core.Authentication authentication) {
        String email = authentication.getName();
        com.whispr.backend.domain.User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        com.whispr.backend.domain.Link link = linkRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Link not found for user"));

        List<MessageDto> messages = messageService.getMessagesForLink(link.getId()).stream()
                .map(msg -> {
                    AuditLog auditLog = auditLogRepository.findByMessageId(msg.getId()).orElse(null);
                    String country = auditLog != null && auditLog.getCountry() != null ? auditLog.getCountry() : "Inconnu 🌐";
                    String deviceHint = auditLog != null ? DeviceUtil.parseDeviceHint(auditLog.getUserAgent()) : "🌐 Navigateur Web";
                    return new MessageDto(
                            msg.getId(),
                            msg.getContent(),
                            msg.getType(),
                            msg.getStatus(),
                            msg.getCreatedAt(),
                            country,
                            deviceHint
                    );
                }).collect(Collectors.toList());
        
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication authentication) {
        String email = authentication.getName();
        messageService.deleteMessage(id, email);
        return ResponseEntity.ok().build();
    }
}
