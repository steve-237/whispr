package com.whispr.backend.service;

import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Message;
import com.whispr.backend.dto.MessageDto;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.repository.MessageRepository;
import com.whispr.backend.util.DeviceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final AiSentimentService aiSentimentService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Message sendMessage(String slug, String content, String hashedIp, String rawIp, String userAgent, String country) {
        Link link = linkRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Link not found"));

        if (!link.getIsActive()) {
            throw new IllegalStateException("Link is currently inactive");
        }

        // 1. Analyse IA du sentiment
        String sentiment = aiSentimentService.analyzeSentiment(content);

        // 2. Sauvegarde du Message
        Message message = Message.builder()
                .link(link)
                .content(content)
                .type("text")
                .status("UNREAD")
                .aiCategory(sentiment)
                .build();
        message = messageRepository.save(message);

        // 3. Sauvegarde de l'Audit (Sécurité)
        AuditLog auditLog = AuditLog.builder()
                .message(message)
                .hashedIp(hashedIp)
                .rawIp(rawIp)
                .userAgent(userAgent)
                .country(country)
                .build();
        auditLogRepository.save(auditLog);

        // 4. Push WebSocket vers l'utilisateur (propriétaire du lien)
        String userEmail = link.getUser().getEmail();
        String deviceHint = DeviceUtil.parseDeviceHint(userAgent);
        String finalCountry = country != null ? country : "Inconnu 🌐";
        
        MessageDto dto = new MessageDto(
                message.getId(),
                message.getContent(),
                message.getType(),
                message.getStatus(),
                message.getCreatedAt(),
                finalCountry,
                deviceHint
        );
        
        // On pousse le message sur le topic personnel de l'utilisateur
        messagingTemplate.convertAndSend("/topic/user/" + userEmail + "/messages", dto);

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

        if (!message.getLink().getUser().getEmail().equals(userEmail)) {
             throw new IllegalStateException("You do not have permission to delete this message");
        }

        auditLogRepository.findByMessageId(messageId)
                .ifPresent(auditLogRepository::delete);

        messageRepository.delete(message);
    }
}
