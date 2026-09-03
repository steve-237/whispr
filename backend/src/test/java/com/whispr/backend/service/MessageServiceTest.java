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
    void sendMessage_ShouldSaveMessageAndAudit_WhenLinkIsActive() {
        // Arrange
        String content = "Hello this is a secret";
        String hashedIp = "hashed123";
        String rawIp = "127.0.0.1";
        String userAgent = "Mozilla/5.0";
        String country = "FR";

        when(linkRepository.findBySlug("tester-slug")).thenReturn(Optional.of(mockLink));
        when(aiSentimentService.analyzeSentiment(content)).thenReturn("POSITIVE");
        
        Message savedMessage = Message.builder()
                .id(UUID.randomUUID())
                .link(mockLink)
                .content(content)
                .type("text")
                .status("UNREAD")
                .aiCategory("POSITIVE")
                .build();
                
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        // Act
        Message result = messageService.sendMessage("tester-slug", content, hashedIp, rawIp, userAgent, country);

        // Assert
        assertNotNull(result);
        assertEquals("UNREAD", result.getStatus());
        assertEquals("POSITIVE", result.getAiCategory());
        
        verify(messageRepository, times(1)).save(any(Message.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/test@whispr.com/messages"), any(Object.class));
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
        
        verify(messageRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void sendMessage_ShouldThrowException_WhenLinkNotFound() {
        // Arrange
        when(linkRepository.findBySlug("unknown-slug")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            messageService.sendMessage("unknown-slug", "content", "hash", "1.1.1.1", "ua", "FR")
        );
        assertEquals("Link not found", exception.getMessage());
    }

    @Test
    void deleteMessage_ShouldDeleteMessage_WhenUserIsOwner() {
        // Arrange
        UUID messageId = UUID.randomUUID();
        Message message = Message.builder()
                .id(messageId)
                .link(mockLink)
                .build();
                
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(auditLogRepository.findByMessageId(messageId)).thenReturn(Optional.empty());

        // Act
        messageService.deleteMessage(messageId, "test@whispr.com");

        // Assert
        verify(messageRepository, times(1)).delete(message);
    }

    @Test
    void deleteMessage_ShouldThrowException_WhenUserIsNotOwner() {
        // Arrange
        UUID messageId = UUID.randomUUID();
        Message message = Message.builder()
                .id(messageId)
                .link(mockLink)
                .build();
                
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> 
            messageService.deleteMessage(messageId, "hacker@whispr.com")
        );
        assertEquals("You do not have permission to delete this message", exception.getMessage());
        
        verify(messageRepository, never()).delete(any());
    }
}
