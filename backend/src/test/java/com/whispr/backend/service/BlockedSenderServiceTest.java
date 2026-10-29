package com.whispr.backend.service;

import com.whispr.backend.domain.*;
import com.whispr.backend.dto.BlockedSenderDto;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.BlockedSenderRepository;
import com.whispr.backend.repository.MessageRepository;
import com.whispr.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockedSenderServiceTest {

    @Mock
    private BlockedSenderRepository blockedSenderRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BlockedSenderService blockedSenderService;

    private User mockUser;
    private Link mockLink;
    private Message mockMessage;
    private AuditLog mockAuditLog;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("victim@whispr.com")
                .pseudo("victimUser")
                .role(Role.USER)
                .build();

        mockLink = Link.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .slug("victim-link")
                .isActive(true)
                .build();

        mockMessage = Message.builder()
                .id(UUID.randomUUID())
                .link(mockLink)
                .content("Insulting message")
                .build();

        mockAuditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .message(mockMessage)
                .hashedIp("a1b2c3d4e5f67890abcdef")
                .rawIp("198.51.100.1")
                .createdAt(ZonedDateTime.now())
                .build();
    }

    @Test
    void blockSenderFromMessage_success() {
        when(userRepository.findByEmailIgnoreCase("victim@whispr.com")).thenReturn(Optional.of(mockUser));
        when(messageRepository.findById(mockMessage.getId())).thenReturn(Optional.of(mockMessage));
        when(auditLogRepository.findByMessageId(mockMessage.getId())).thenReturn(Optional.of(mockAuditLog));
        when(blockedSenderRepository.existsByUserIdAndBlockedHashedIp(mockUser.getId(), "a1b2c3d4e5f67890abcdef")).thenReturn(false);
        when(blockedSenderRepository.save(any(BlockedSender.class))).thenAnswer(invocation -> {
            BlockedSender b = invocation.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        BlockedSenderDto result = blockedSenderService.blockSenderFromMessage(mockMessage.getId(), "victim@whispr.com");

        assertNotNull(result);
        assertNotNull(result.id());
        assertTrue(result.displayName().contains("a1b2c3d4"));
        verify(blockedSenderRepository).save(any(BlockedSender.class));
    }

    @Test
    void blockSenderFromMessage_forbiddenWhenNotOwner() {
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@whispr.com")
                .build();

        when(userRepository.findByEmailIgnoreCase("other@whispr.com")).thenReturn(Optional.of(otherUser));
        when(messageRepository.findById(mockMessage.getId())).thenReturn(Optional.of(mockMessage));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                blockedSenderService.blockSenderFromMessage(mockMessage.getId(), "other@whispr.com"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(blockedSenderRepository, never()).save(any());
    }

    @Test
    void unblockSender_success() {
        UUID blockId = UUID.randomUUID();
        BlockedSender block = BlockedSender.builder()
                .id(blockId)
                .user(mockUser)
                .blockedHashedIp("a1b2c3d4")
                .build();

        when(userRepository.findByEmailIgnoreCase("victim@whispr.com")).thenReturn(Optional.of(mockUser));
        when(blockedSenderRepository.findByIdAndUserId(blockId, mockUser.getId())).thenReturn(Optional.of(block));

        blockedSenderService.unblockSender(blockId, "victim@whispr.com");

        verify(blockedSenderRepository).delete(block);
    }

    @Test
    void isSenderBlocked_returnsTrueWhenBlocked() {
        when(blockedSenderRepository.existsByUserIdAndBlockedHashedIp(mockUser.getId(), "bad-ip-hash")).thenReturn(true);

        boolean blocked = blockedSenderService.isSenderBlocked(mockUser.getId(), "bad-ip-hash");
        assertTrue(blocked);
    }
}
