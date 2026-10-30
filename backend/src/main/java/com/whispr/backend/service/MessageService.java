package com.whispr.backend.service;

import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Message;
import com.whispr.backend.domain.User;
import com.whispr.backend.dto.MessageDto;
import com.whispr.backend.dto.MessageSendRequest;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.repository.MessageRepository;
import com.whispr.backend.repository.UserRepository;
import com.whispr.backend.util.ClientMetadata;
import com.whispr.backend.util.DeviceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final LinkRepository linkRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AiSentimentService aiSentimentService;
    private final ModerationService moderationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final BlockedSenderService blockedSenderService;

    @Transactional
    public Message sendMessage(String slug, MessageSendRequest request, ClientMetadata metadata) {
        return sendMessage(
                slug,
                request.content(),
                request.type(),
                request.audioUrl(),
                request.audioDurationSeconds(),
                request.voiceFilter(),
                metadata.hashedIp(),
                metadata.rawIp(),
                metadata.userAgent(),
                metadata.country(),
                metadata.city(),
                metadata.referrer()
        );
    }

    @Transactional
    public Message sendMessage(String slug, String content, String type, ClientMetadata metadata) {
        return sendMessage(
                slug,
                content,
                type,
                metadata.hashedIp(),
                metadata.rawIp(),
                metadata.userAgent(),
                metadata.country(),
                metadata.city(),
                metadata.referrer()
        );
    }

    @Transactional
    public Message sendMessage(String slug, String content, String type, String hashedIp, String rawIp, String userAgent, String country, String city, String referrer) {
        return sendMessage(slug, content, type, null, null, null, hashedIp, rawIp, userAgent, country, city, referrer);
    }

    @Transactional
    public Message sendMessage(String slug, String content, String type, String audioUrl, Integer audioDurationSeconds, String voiceFilter, String hashedIp, String rawIp, String userAgent, String country, String city, String referrer) {
        if (moderationService != null && moderationService.isToxic(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre message enfreint nos règles de bienveillance et a été bloqué.");
        }

        Link link = linkRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));

        if (!link.getIsActive()) {
            throw new IllegalStateException("Link is currently inactive");
        }

        // Anti-Spam / Shadowban (US-08: Rejet silencieux d'expéditeur bloqué)
        if (blockedSenderService != null && blockedSenderService.isSenderBlocked(link.getUser().getId(), hashedIp)) {
            return Message.builder()
                    .link(link)
                    .content(content)
                    .type(type)
                    .status("DELIVERED")
                    .build();
        }

        // Anti-Spam (Rate Limiting)
        if (rawIp != null) {
            int recentMessages = auditLogRepository.countByRawIpAndCreatedAtAfter(rawIp, ZonedDateTime.now().minusHours(1));
            if (recentMessages >= 50) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Vous envoyez trop de messages. Veuillez patienter.");
            }
        }

        // 1. Analyse IA du sentiment
        String sentiment = aiSentimentService.analyzeSentiment(content);

        // 2. Sauvegarde du Message
        String finalType;
        if ((audioUrl != null && !audioUrl.isBlank()) || "AUDIO".equalsIgnoreCase(type)) {
            finalType = "AUDIO";
            if (content == null || content.isBlank()) {
                String filterLabel = (voiceFilter != null) ? voiceFilter : "NORMAL";
                content = "🎙️ Mémo vocal (" + filterLabel + ")";
            }
        } else if (type != null && type.equalsIgnoreCase("SUPER_WHISPR")) {
            finalType = "text";
        } else {
            finalType = "text";
        }

        Message message = Message.builder()
                .link(link)
                .content(content)
                .type(finalType)
                .status("UNREAD")
                .aiCategory(sentiment)
                .audioUrl(audioUrl)
                .audioDurationSeconds(audioDurationSeconds)
                .voiceFilter(voiceFilter)
                .build();
        message = messageRepository.save(message);

        // 3. Sauvegarde de l'Audit (Sécurité & Géolocalisation Réelle)
        AuditLog auditLog = AuditLog.builder()
                .message(message)
                .hashedIp(hashedIp)
                .rawIp(rawIp)
                .userAgent(userAgent)
                .country(country)
                .city(city)
                .referrer(referrer)
                .build();
        auditLogRepository.save(auditLog);
        message.setAuditLog(auditLog);

        // 4. Push WebSocket vers l'utilisateur (propriétaire du lien)
        String userEmail = link.getUser().getEmail();
        String deviceHint = DeviceUtil.parseDeviceHint(userAgent);
        String finalCountry = country != null ? country : "Inconnu 🌍";

        MessageDto dto = new MessageDto(
                message.getId(),
                message.getContent(),
                message.getType(),
                message.getStatus(),
                message.getCreatedAt(),
                finalCountry,
                deviceHint,
                message.isRead(),
                message.getAiCategory()
        );

        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/user/" + userEmail + "/messages", dto);
        }

        return message;
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getInboxMessagesForUser(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Link link = linkRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found for user"));

        return messageRepository.findByLinkIdWithAuditLogOrderByCreatedAtDesc(link.getId())
                .stream()
                .map(this::toDto)
                .filter(m -> (m.tipAmountEur() == null || m.isTipPaid()) && !"PENDING_PAYMENT".equals(m.status()) && !"PENDING_TIP".equals(m.status()))
                .collect(Collectors.toList());
    }

    public MessageDto toDto(Message message) {
        AuditLog audit = message.getAuditLog();
        String country = (audit != null && audit.getCountry() != null) ? audit.getCountry() : "Inconnu 🌍";
        String deviceHint = (audit != null) ? DeviceUtil.parseDeviceHint(audit.getUserAgent()) : "🌍 Navigateur Web";
        return new MessageDto(
                message.getId(),
                message.getContent(),
                message.getType(),
                message.getStatus(),
                message.getCreatedAt(),
                country,
                deviceHint,
                message.isRead(),
                message.getAiCategory()
        );
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesForLink(UUID linkId) {
        return messageRepository.findByLinkIdOrderByCreatedAtDesc(linkId);
    }

    @Transactional
    public void deleteMessage(UUID messageId, String userEmail) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getLink().getUser().getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this message");
        }

        auditLogRepository.findByMessageId(messageId)
                .ifPresent(auditLogRepository::delete);

        messageRepository.delete(message);
    }

    @Transactional
    public void markAsRead(UUID messageId, String userEmail) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getLink().getUser().getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update this message");
        }

        message.setRead(true);
        message.setStatus("READ");
        messageRepository.save(message);
    }
}
