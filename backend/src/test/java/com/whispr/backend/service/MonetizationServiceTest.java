package com.whispr.backend.service;

import com.whispr.backend.domain.Role;
import com.whispr.backend.domain.Transaction;
import com.whispr.backend.domain.User;
import com.whispr.backend.domain.Wallet;
import com.whispr.backend.dto.CheckoutSimulationRequest;
import com.whispr.backend.dto.WalletDto;
import com.whispr.backend.repository.TransactionRepository;
import com.whispr.backend.repository.UserRepository;
import com.whispr.backend.repository.WalletRepository;
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
class MonetizationServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClueService clueService;

    @Mock
    private VisitorAnalyticsService visitorAnalyticsService;

    @InjectMocks
    private MonetizationService monetizationService;

    private User mockUser;
    private Wallet mockWallet;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("monetization@whispr.com")
                .pseudo("cryptoUser")
                .role(Role.USER)
                .build();

        mockWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .coins(30)
                .totalSpentEur(BigDecimal.ZERO)
                .isPro(false)
                .earningsEur(BigDecimal.ZERO)
                .referralCount(0)
                .affiliateCode("cryptoUser")
                .build();
    }

    @Test
    void getOrCreateWallet_ShouldReturnExisting_WhenPresent() {
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockWallet));

        Wallet wallet = monetizationService.getOrCreateWallet(mockUser);

        assertEquals(mockWallet, wallet);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void getOrCreateWallet_ShouldCreateNewWith30Coins_WhenAbsent() {
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet wallet = monetizationService.getOrCreateWallet(mockUser);

        assertNotNull(wallet);
        assertEquals(30, wallet.getCoins());
        assertFalse(wallet.isPro());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void simulateCheckout_ShouldAdd50Coins_ForPackCoins50() {
        when(userRepository.findByEmailIgnoreCase("monetization@whispr.com")).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockWallet));

        CheckoutSimulationRequest request = new CheckoutSimulationRequest("COINS_50");
        WalletDto result = monetizationService.simulateCheckout("monetization@whispr.com", request);

        assertEquals(80, result.coins()); // 30 + 50
        assertEquals(new BigDecimal("0.99"), result.totalSpentEur());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void simulateCheckout_ShouldActivatePro_ForProMonthly() {
        when(userRepository.findByEmailIgnoreCase("monetization@whispr.com")).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockWallet));

        CheckoutSimulationRequest request = new CheckoutSimulationRequest("PRO_MONTHLY");
        WalletDto result = monetizationService.simulateCheckout("monetization@whispr.com", request);

        assertTrue(result.isPro());
        assertEquals(80, result.coins()); // 30 + 50 bonus
        assertEquals(new BigDecimal("4.99"), result.totalSpentEur());
    }

    @Test
    void simulateCheckout_ShouldThrowBadRequest_ForInvalidPack() {
        when(userRepository.findByEmailIgnoreCase("monetization@whispr.com")).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockWallet));

        CheckoutSimulationRequest request = new CheckoutSimulationRequest("UNKNOWN_PACK");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                monetizationService.simulateCheckout("monetization@whispr.com", request)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
