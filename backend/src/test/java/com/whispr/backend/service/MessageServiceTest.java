package com.whispr.backend.service;

import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Message;
import com.whispr.backend.domain.User;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private AiSentimentService aiSentimentService;

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
        // Arrange
        when(linkRepository.findBySlug("tester-slug")).thenReturn(Optional.of(mockLink));
        when(auditLogRepository.countByRawIpAndCreatedAtAfter(eq("127.0.0.1"), any(LocalDateTime.class))).thenReturn(2);
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

        // Act
        Message result = messageService.sendMessage("tester-slug", "hello", "hash", "127.0.0.1", "ua", "FR");

        // Assert
        assertNotNull(result);
        verify(messageRepository, times(1)).save(any(Message.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void sendMessage_ShouldThrowTooManyRequests_WhenSpamLimitReached() {
        // Arrange
        when(linkRepository.findBySlug("tester-slug")).thenReturn(Optional.of(mockLink));
        // Return 5 to trigger rate limit (>= 5)
        when(auditLogRepository.countByRawIpAndCreatedAtAfter(eq("127.0.0.1"), any(LocalDateTime.class))).thenReturn(5);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> 
            messageService.sendMessage("tester-slug", "spam", "hash", "127.0.0.1", "ua", "FR")
        );
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        assertEquals("Vous envoyez trop de messages. Veuillez patienter.", exception.getReason());
        
        verify(messageRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void sendMessage_ShouldThrowException_WhenLinkIsInactive() {
        // Arrange
        mockLink.setIsActive(false);
        when(linkRepository.findBySlug("tester-slug")).thenReturn(Optional.of(mockLink));

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> 
            messageService.sendMessage("tester-slug", "content", "hash", "1.1.1.1", "ua", "FR")
        );
        assertEquals("Link is currently inactive", exception.getMessage());
    }
}
