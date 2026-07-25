package com.whispr.backend.service;

import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Message;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final LinkRepository linkRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public Message sendMessage(String slug, String content, String hashedIp, String userAgent, String country) {
        Link link = linkRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Link not found"));

        if (!link.getIsActive()) {
            throw new IllegalStateException("Link is currently inactive");
        }

        Message message = Message.builder()
                .link(link)
                .content(content)
                .type("text")
                .status("delivered") // in future: pending AI moderation
                .build();
        message = messageRepository.save(message);

        AuditLog auditLog = AuditLog.builder()
                .message(message)
                .hashedIp(hashedIp)
                .userAgent(userAgent)
                .country(country)
                .build();
        auditLogRepository.save(auditLog);

        return message;
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesForLink(UUID linkId) {
        return messageRepository.findByLinkIdOrderByCreatedAtDesc(linkId);
    }

    @Transactional
    public void deleteMessage(UUID messageId, String userEmail) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Vérifier que l'utilisateur qui demande la suppression est bien le propriétaire du lien
        if (!message.getLink().getUser().getEmail().equals(userEmail)) {
            // Permettre aux admins de supprimer s'ils le souhaitent, ou juste bloquer
            if (message.getLink().getUser().getRole() != com.whispr.backend.domain.Role.ADMIN 
                    && !message.getLink().getUser().getEmail().equals(userEmail)) {
                 // But wait, the admin check should be on the requesting user, not the link's user.
                 // We will just do a simple check for now: only the owner can delete.
            }
            if (!message.getLink().getUser().getEmail().equals(userEmail)) {
                 throw new IllegalStateException("You do not have permission to delete this message");
            }
        }

        // Supprimer l'audit log associé s'il existe
        auditLogRepository.findByMessageId(messageId)
                .ifPresent(auditLogRepository::delete);

        // Supprimer le message
        messageRepository.delete(message);
    }
}
