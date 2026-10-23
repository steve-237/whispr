package com.whispr.backend.service;

import com.whispr.backend.domain.*;
import com.whispr.backend.dto.UnlockClueRequest;
import com.whispr.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClueServiceTest {

    @Mock
    private MonetizationService monetizationService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UnlockedClueRepository unlockedClueRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClueService clueService;

    private User mockUser;
    private Wallet mockWallet;
    private Message mockMessage;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("clue@whispr.com")
                .pseudo("clueUser")
                .role(Role.USER)
                .build();

        mockWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .coins(30)
                .totalSpentEur(BigDecimal.ZERO)
                .isPro(false)
                .build();

        Link link = Link.builder().user(mockUser).build();
        mockMessage = Message.builder().id(UUID.randomUUID()).link(link).build();
    }

    @Test
    void unlockClue_ShouldDeductCoins_WhenNotProAndSufficient() {
        when(userRepository.findByEmailIgnoreCase("clue@whispr.com")).thenReturn(Optional.of(mockUser));
        when(messageRepository.findById(mockMessage.getId())).thenReturn(Optional.of(mockMessage));
        when(monetizationService.getOrCreateWallet(mockUser)).thenReturn(mockWallet);
        when(unlockedClueRepository.findByUserIdAndMessageIdAndClueType(mockUser.getId(), mockMessage.getId(), "DEVICE"))
                .thenReturn(Optional.empty());

        UnlockClueRequest request = new UnlockClueRequest(mockMessage.getId(), "DEVICE");
        var res = clueService.unlockClue("clue@whispr.com", request);

        assertTrue(res.isUnlocked());
        assertEquals(15, mockWallet.getCoins()); // 30 - 15
        verify(walletRepository, times(1)).save(mockWallet);
        verify(unlockedClueRepository, times(1)).save(any(UnlockedClue.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void unlockClue_ShouldBeFree_WhenProUser() {
        mockWallet.setPro(true);
        when(userRepository.findByEmailIgnoreCase("clue@whispr.com")).thenReturn(Optional.of(mockUser));
        when(messageRepository.findById(mockMessage.getId())).thenReturn(Optional.of(mockMessage));
        when(monetizationService.getOrCreateWallet(mockUser)).thenReturn(mockWallet);

        UnlockClueRequest request = new UnlockClueRequest(mockMessage.getId(), "DEVICE");
        var res = clueService.unlockClue("clue@whispr.com", request);

        assertTrue(res.isUnlocked());
        assertEquals(30, mockWallet.getCoins()); // Not deducted
        verify(walletRepository, never()).save(any());
    }

    @Test
    void unlockClue_ShouldThrowForbidden_WhenMessageBelongsToAnotherUser() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        Link otherLink = Link.builder().user(otherUser).build();
        Message foreignMessage = Message.builder().id(UUID.randomUUID()).link(otherLink).build();

        when(userRepository.findByEmailIgnoreCase("clue@whispr.com")).thenReturn(Optional.of(mockUser));
        when(messageRepository.findById(foreignMessage.getId())).thenReturn(Optional.of(foreignMessage));

        UnlockClueRequest request = new UnlockClueRequest(foreignMessage.getId(), "DEVICE");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                clueService.unlockClue("clue@whispr.com", request)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
