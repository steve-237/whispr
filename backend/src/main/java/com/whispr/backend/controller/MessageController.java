package com.whispr.backend.controller;

import com.whispr.backend.dto.BlockedSenderDto;
import com.whispr.backend.dto.MessageDto;
import com.whispr.backend.dto.MessageSendRequest;
import com.whispr.backend.service.BlockedSenderService;
import com.whispr.backend.service.MessageService;
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
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final BlockedSenderService blockedSenderService;
    private final ClientMetadataResolver clientMetadataResolver;

    @PostMapping("/send/{slug}")
    public ResponseEntity<Void> sendMessage(
            @PathVariable String slug,
            @RequestBody MessageSendRequest request,
            HttpServletRequest httpRequest) {

        ClientMetadata metadata = clientMetadataResolver.resolve(httpRequest);
        messageService.sendMessage(slug.toLowerCase(), request, metadata);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<MessageDto>> getInbox(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(messageService.getInboxMessagesForUser(email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        messageService.deleteMessage(id, email);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        messageService.markAsRead(id, email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<BlockedSenderDto> blockSender(
            @PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(blockedSenderService.blockSenderFromMessage(id, email));
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<BlockedSenderDto>> getBlockedSenders(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(blockedSenderService.getBlockedSenders(email));
    }

    @DeleteMapping("/blocks/{id}")
    public ResponseEntity<Void> unblockSender(
            @PathVariable UUID id,
            Authentication authentication) {
        String email = authentication.getName();
        blockedSenderService.unblockSender(id, email);
        return ResponseEntity.noContent().build();
    }
}
