package com.whispr.backend.controller;

import com.whispr.backend.domain.Message;
import com.whispr.backend.dto.MessageDto;
import com.whispr.backend.dto.MessageSendRequest;
import com.whispr.backend.service.MessageService;
import com.whispr.backend.service.UserService;
import com.whispr.backend.repository.LinkRepository;
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

    @PostMapping("/send/{slug}")
    public ResponseEntity<Void> sendMessage(
            @PathVariable String slug,
            @RequestBody MessageSendRequest request,
            HttpServletRequest httpRequest) {
        
        // Simulating IP hashing for MVP
        String ip = httpRequest.getRemoteAddr();
        String hashedIp = Integer.toHexString(ip.hashCode());
        String userAgent = httpRequest.getHeader("User-Agent");
        
        // Try to get country from Cloudflare header or mock it
        String country = httpRequest.getHeader("CF-IPCountry");
        if (country == null) {
            country = "France (Simulé)";
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
                .map(msg -> new MessageDto(
                        msg.getId(),
                        msg.getContent(),
                        msg.getType(),
                        msg.getStatus(),
                        msg.getCreatedAt()
                )).collect(Collectors.toList());
        
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
