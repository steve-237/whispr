package com.whispr.backend.service;

import com.whispr.backend.domain.PayoutRequest;
import com.whispr.backend.domain.Role;
import com.whispr.backend.domain.Transaction;
import com.whispr.backend.domain.User;
import com.whispr.backend.domain.Wallet;
import com.whispr.backend.dto.CreatePayoutRequest;
import com.whispr.backend.dto.PayoutResponseDto;
import com.whispr.backend.dto.UpdatePayoutStatusRequest;
import com.whispr.backend.repository.PayoutRequestRepository;
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
class PayoutServiceTest {

    @Mock
    private PayoutRequestRepository payoutRequestRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PayoutService payoutService;

    private User mockUser;
    private Wallet mockWallet;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("creator@whispr.com")
                .pseudo("creatorStar")
                .role(Role.USER)
                .build();

        mockWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .coins(100)
                .earningsEur(new BigDecimal("50.00"))
                .referralCount(5)
                .build();
    }

    @Test
    void createPayoutRequest_success() {
        when(userRepository.findByEmailIgnoreCase("creator@whispr.com")).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockWallet));
        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenAnswer(invocation -> {
            PayoutRequest pr = invocation.getArgument(0);
            pr.setId(UUID.randomUUID());
            return pr;
        });

        CreatePayoutRequest req = new CreatePayoutRequest(new BigDecimal("25.00"), "PAYPAL", "creator@paypal.me");
        PayoutResponseDto response = payoutService.createPayoutRequest("creator@whispr.com", req);

        assertNotNull(response);
        assertEquals("PENDING", response.status());
        assertEquals("PAYPAL", response.method());
        assertEquals(new BigDecimal("25.00"), response.amountEur());
        assertEquals(new BigDecimal("25.00"), mockWallet.getEarningsEur());

        verify(walletRepository).save(mockWallet);
        verify(transactionRepository).save(any(Transaction.class));
        verify(payoutRequestRepository).save(any(PayoutRequest.class));
    }

    @Test
    void createPayoutRequest_belowMinimum_throwsBadRequest() {
        CreatePayoutRequest req = new CreatePayoutRequest(new BigDecimal("15.00"), "PAYPAL", "creator@paypal.me");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> payoutService.createPayoutRequest("creator@whispr.com", req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("minimum"));
        verifyNoInteractions(walletRepository);
    }

    @Test
    void createPayoutRequest_insufficientBalance_throwsBadRequest() {
        when(userRepository.findByEmailIgnoreCase("creator@whispr.com")).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockWallet));

        CreatePayoutRequest req = new CreatePayoutRequest(new BigDecimal("60.00"), "IBAN", "FR7612345678901234567890123");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> payoutService.createPayoutRequest("creator@whispr.com", req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("insuffisant"));
        assertEquals(new BigDecimal("50.00"), mockWallet.getEarningsEur());
    }

    @Test
    void updatePayoutStatus_rejected_refundsUser() {
        UUID payoutId = UUID.randomUUID();
        PayoutRequest existingPayout = PayoutRequest.builder()
                .id(payoutId)
                .user(mockUser)
                .amountEur(new BigDecimal("30.00"))
                .method("PAYPAL")
                .payoutDetails("creator@paypal.me")
                .status("PENDING")
                .build();

        when(payoutRequestRepository.findById(payoutId)).thenReturn(Optional.of(existingPayout));
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockWallet));
        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePayoutStatusRequest updateReq = new UpdatePayoutStatusRequest("REJECTED", "Coordonnées PayPal erronées");
        PayoutResponseDto response = payoutService.updatePayoutStatus(payoutId, updateReq);

        assertEquals("REJECTED", response.status());
        assertEquals("Coordonnées PayPal erronées", response.adminNote());
        // Solde initial 50 + 30 remboursés = 80
        assertEquals(new BigDecimal("80.00"), mockWallet.getEarningsEur());

        verify(walletRepository).save(mockWallet);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void updatePayoutStatus_paid_noRefund() {
        UUID payoutId = UUID.randomUUID();
        PayoutRequest existingPayout = PayoutRequest.builder()
                .id(payoutId)
                .user(mockUser)
                .amountEur(new BigDecimal("30.00"))
                .method("PAYPAL")
                .payoutDetails("creator@paypal.me")
                .status("PENDING")
                .build();

        when(payoutRequestRepository.findById(payoutId)).thenReturn(Optional.of(existingPayout));
        when(payoutRequestRepository.save(any(PayoutRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePayoutStatusRequest updateReq = new UpdatePayoutStatusRequest("PAID", "Virement effectué");
        PayoutResponseDto response = payoutService.updatePayoutStatus(payoutId, updateReq);

        assertEquals("PAID", response.status());
        assertEquals("Virement effectué", response.adminNote());
        assertEquals(new BigDecimal("50.00"), mockWallet.getEarningsEur());

        verify(walletRepository, never()).save(any());
    }
}
