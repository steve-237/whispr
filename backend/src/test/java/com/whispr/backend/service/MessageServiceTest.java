package com.whispr.backend.service;

import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Message;
import com.whispr.backend.domain.User;
import com.whispr.backend.dto.MessageDto;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.repository.MessageRepository;
import com.whispr.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiSentimentService aiSentimentService;

    @Mock
    private ModerationService moderationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageService messageService;

    private User mockUser;
    private Link mockLink;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@whispr.com")
                .pseudo("tester")
                .role(com.whispr.backend.domain.Role.USER)
                .build();

        mockLink = Link.builder()
                .id(UUID.randomUUID())
                .slug("tester-slug")
                .isActive(true)
                .user(mockUser)
                .build();
    }

    @Test
    void sendMessage_ShouldSaveMessageAndAudit_WhenValid() {
        when(moderationService.isToxic(anyString())).thenReturn(false);
        when(linkRepository.findBySlug("tester-slug")).thenReturn(Optional.of(mockLink));
        when(auditLogRepository.countByRawIpAndCreatedAtAfter(eq("127.0.0.1"), any(ZonedDateTime.class))).thenReturn(2);
        when(aiSentimentService.analyzeSentiment(anyString())).thenReturn("POSITIVE");

        Message savedMessage = Message.builder()
                .id(UUID.randomUUID())
                .link(mockLink)
                .content("hello")
                .type("text")
                .status("UNREAD")
                .aiCategory("POSITIVE")
                .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        Message result = messageService.sendMessage("tester-slug", "hello", "text", "hash", "127.0.0.1", "ua", "FR", "Paris", "https://instagram.com");

        assertNotNull(result);
        verify(messageRepository, times(1)).save(any(Message.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void sendMessage_ShouldThrowBadRequest_WhenContentIsToxic() {
        when(moderationService.isToxic("hate content")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                messageService.sendMessage("tester-slug", "hate content", "text", "hash", "127.0.0.1", "ua", "FR", "Paris", "direct")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_ShouldThrowTooManyRequests_WhenSpamLimitReached() {
        when(moderationService.isToxic(anyString())).thenReturn(false);
        when(linkRepository.findBySlug("tester-slug")).thenReturn(Optional.of(mockLink));
        when(auditLogRepository.countByRawIpAndCreatedAtAfter(eq("127.0.0.1"), any(ZonedDateTime.class))).thenReturn(50);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                messageService.sendMessage("tester-slug", "spam", "text", "hash", "127.0.0.1", "ua", "FR", "Paris", "direct")
        );
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        assertEquals("Vous envoyez trop de messages. Veuillez patienter.", exception.getReason());

        verify(messageRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void sendMessage_ShouldThrowException_WhenLinkIsInactive() {
        when(moderationService.isToxic(anyString())).thenReturn(false);
        mockLink.setIsActive(false);
        when(linkRepository.findBySlug("tester-slug")).thenReturn(Optional.of(mockLink));

        Exception exception = assertThrows(IllegalStateException.class, () ->
                messageService.sendMessage("tester-slug", "content", "text", "hash", "1.1.1.1", "ua", "FR", "Paris", "direct")
        );
        assertEquals("Link is currently inactive", exception.getMessage());
    }

    @Test
    void getInboxMessagesForUser_ShouldReturnMessageDtosWithoutNPlusOne() {
        when(userRepository.findByEmailIgnoreCase("test@whispr.com")).thenReturn(Optional.of(mockUser));
        when(linkRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockLink));

        AuditLog audit = AuditLog.builder()
                .country("France 🇫🇷")
                .userAgent("Mozilla/5.0 (iPhone)")
                .build();

        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .content("secret whisper")
                .type("text")
                .status("UNREAD")
                .auditLog(audit)
                .build();

        when(messageRepository.findByLinkIdWithAuditLogOrderByCreatedAtDesc(mockLink.getId()))
                .thenReturn(List.of(msg));

        List<MessageDto> inbox = messageService.getInboxMessagesForUser("test@whispr.com");

        assertEquals(1, inbox.size());
        assertEquals("secret whisper", inbox.get(0).content());
        assertEquals("France 🇫🇷", inbox.get(0).country());
        verify(auditLogRepository, never()).findByMessageId(any());
    }

    @Test
    void deleteMessage_ShouldDelete_WhenUserIsOwner() {
        UUID messageId = UUID.randomUUID();
        Message message = Message.builder()
                .id(messageId)
                .link(mockLink)
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(auditLogRepository.findByMessageId(messageId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> messageService.deleteMessage(messageId, "test@whispr.com"));
        verify(messageRepository, times(1)).delete(message);
    }

    @Test
    void deleteMessage_ShouldThrowForbidden_WhenUserIsNotOwner() {
        UUID messageId = UUID.randomUUID();
        Message message = Message.builder()
                .id(messageId)
                .link(mockLink)
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                messageService.deleteMessage(messageId, "stranger@other.com")
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(messageRepository, never()).delete(any());
    }

    @Test
    void markAsRead_ShouldUpdateStatus_WhenUserIsOwner() {
        UUID messageId = UUID.randomUUID();
        Message message = Message.builder()
                .id(messageId)
                .link(mockLink)
                .isRead(false)
                .status("UNREAD")
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        messageService.markAsRead(messageId, "test@whispr.com");

        assertTrue(message.isRead());
        assertEquals("READ", message.getStatus());
        verify(messageRepository, times(1)).save(message);
    }
}
