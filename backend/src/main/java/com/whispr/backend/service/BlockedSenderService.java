package com.whispr.backend.service;

import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.BlockedSender;
import com.whispr.backend.domain.Message;
import com.whispr.backend.domain.User;
import com.whispr.backend.dto.BlockedSenderDto;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.BlockedSenderRepository;
import com.whispr.backend.repository.MessageRepository;
import com.whispr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockedSenderService {

    private final BlockedSenderRepository blockedSenderRepository;
    private final MessageRepository messageRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public BlockedSenderDto blockSenderFromMessage(UUID messageId, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message introuvable"));

        if (!message.getLink().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez pas bloquer l'expéditeur d'un message qui ne vous appartient pas");
        }

        AuditLog auditLog = auditLogRepository.findByMessageId(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informations de sécurité du message indisponibles pour le blocage"));

        String hashedIp = auditLog.getHashedIp();
        if (hashedIp == null || hashedIp.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empreinte numérique de l'expéditeur introuvable");
        }

        if (blockedSenderRepository.existsByUserIdAndBlockedHashedIp(user.getId(), hashedIp)) {
            log.info("Expéditeur avec IP hashée {} déjà bloqué par {}", hashedIp, user.getPseudo());
            return new BlockedSenderDto(null, "Expéditeur #" + hashedIp.substring(0, Math.min(8, hashedIp.length())), "Déjà bloqué", auditLog.getCreatedAt());
        }

        BlockedSender block = BlockedSender.builder()
                .user(user)
                .blockedHashedIp(hashedIp)
                .reason("Bloqué suite au message ID: " + messageId)
                .build();

        BlockedSender saved = blockedSenderRepository.save(block);
        log.info("Expéditeur {} bloqué par {} (User ID: {})", hashedIp, user.getPseudo(), user.getId());

        String display = "Expéditeur #" + hashedIp.substring(0, Math.min(8, hashedIp.length()));
        return new BlockedSenderDto(saved.getId(), display, saved.getReason(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<BlockedSenderDto> getBlockedSenders(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        return blockedSenderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(b -> {
                    String hash = b.getBlockedHashedIp();
                    String display = "Expéditeur #" + (hash != null ? hash.substring(0, Math.min(8, hash.length())) : "anonyme");
                    return new BlockedSenderDto(b.getId(), display, b.getReason(), b.getCreatedAt());
                })
                .toList();
    }

    @Transactional
    public void unblockSender(UUID blockId, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        BlockedSender block = blockedSenderRepository.findByIdAndUserId(blockId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blocage introuvable"));

        blockedSenderRepository.delete(block);
        log.info("Déblocage de l'expéditeur {} par {}", block.getBlockedHashedIp(), user.getPseudo());
    }

    @Transactional(readOnly = true)
    public boolean isSenderBlocked(UUID recipientUserId, String hashedIp) {
        if (recipientUserId == null || hashedIp == null || hashedIp.isBlank()) {
            return false;
        }
        return blockedSenderRepository.existsByUserIdAndBlockedHashedIp(recipientUserId, hashedIp);
    }
}
