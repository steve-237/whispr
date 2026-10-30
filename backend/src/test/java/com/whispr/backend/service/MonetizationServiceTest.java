package com.whispr.backend.service;

import com.whispr.backend.domain.Role;
import com.whispr.backend.domain.Transaction;
import com.whispr.backend.domain.User;
import com.whispr.backend.domain.Wallet;
import com.whispr.backend.dto.CheckoutSessionResponse;
import com.whispr.backend.dto.CheckoutSimulationRequest;
import com.whispr.backend.dto.CreateCheckoutSessionRequest;
import com.whispr.backend.dto.WalletDto;
import com.whispr.backend.repository.TransactionRepository;
import com.whispr.backend.repository.UserRepository;
import com.whispr.backend.repository.WalletRepository;
import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Message;
import com.whispr.backend.dto.CreateTipCheckoutRequest;
import com.whispr.backend.dto.TipCheckoutResponse;
import com.whispr.backend.dto.CreateSuperWhisprCheckoutRequest;
import com.whispr.backend.dto.SuperWhisprCheckoutResponse;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.repository.MessageRepository;
import com.whispr.backend.util.ClientMetadata;
import com.whispr.backend.util.ClientMetadataResolver;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AiSentimentService aiSentimentService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageService messageService;

    @Mock
    private ClientMetadataResolver clientMetadataResolver;


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
    void createCheckoutSession_ShouldReturnSimulatedUrl_WhenStripeKeyNotConfigured() {
        when(userRepository.findByEmailIgnoreCase("monetization@whispr.com")).thenReturn(Optional.of(mockUser));

        CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest("COINS_50", null);
        CheckoutSessionResponse response = monetizationService.createCheckoutSession("monetization@whispr.com", request);

        assertNotNull(response);
        assertNotNull(response.sessionId());
        assertTrue(response.checkoutUrl().contains("payment=success"));
    }

    @Test
    void createCheckoutSession_ShouldThrowBadRequest_ForInvalidPack() {
        when(userRepository.findByEmailIgnoreCase("monetization@whispr.com")).thenReturn(Optional.of(mockUser));

        CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest("INVALID_PACK", null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                monetizationService.createCheckoutSession("monetization@whispr.com", request)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void handleWebhook_ShouldCreditCoinsAndSaveTransaction_OnCheckoutSessionCompleted() {
        when(transactionRepository.existsByStripeEventId("evt_test_12345")).thenReturn(false);
        when(transactionRepository.existsByStripeSessionId("cs_test_67890")).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("monetization@whispr.com")).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockWallet));

        String payload = """
            {
              "api_version": "2024-06-20",
              "id": "evt_test_12345",
              "type": "checkout.session.completed",
              "data": {
                "object": {
                  "id": "cs_test_67890",
                  "amount_total": 99,
                  "customer_email": "monetization@whispr.com",
                  "metadata": {
                    "packId": "COINS_50",
                    "coins": "50",
                    "userEmail": "monetization@whispr.com"
                  }
                }
              }
            }
            """;

        monetizationService.handleWebhook(payload, null);

        assertEquals(80, mockWallet.getCoins()); // 30 + 50
        assertEquals(new BigDecimal("0.99"), mockWallet.getTotalSpentEur());
        verify(walletRepository, times(1)).save(mockWallet);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void handleWebhook_ShouldBeIdempotent_WhenEventAlreadyProcessed() {
        when(transactionRepository.existsByStripeEventId("evt_already_done")).thenReturn(true);

        String payload = """
            {
              "api_version": "2024-06-20",
              "id": "evt_already_done",
              "type": "checkout.session.completed",
              "data": {
                "object": {
                  "id": "cs_already_done"
                }
              }
            }
            """;

        monetizationService.handleWebhook(payload, null);

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void handleWebhook_ShouldCreditAffiliateCommission_WhenReferrerPresent() {
        UUID referrerId = UUID.randomUUID();
        User referrerUser = User.builder()
                .id(referrerId)
                .email("creator@whispr.com")
                .pseudo("creatorVIP")
                .role(Role.USER)
                .build();

        Wallet referrerWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .user(referrerUser)
                .coins(100)
                .earningsEur(BigDecimal.ZERO)
                .referralCount(0)
                .build();

        when(transactionRepository.existsByStripeEventId("evt_affiliate_1")).thenReturn(false);
        when(transactionRepository.existsByStripeSessionId("cs_affiliate_1")).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("monetization@whispr.com")).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockWallet));
        when(userRepository.findById(referrerId)).thenReturn(Optional.of(referrerUser));
        when(walletRepository.findByUserId(referrerId)).thenReturn(Optional.of(referrerWallet));

        String payload = String.format("""
            {
              "api_version": "2024-06-20",
              "id": "evt_affiliate_1",
              "type": "checkout.session.completed",
              "data": {
                "object": {
                  "id": "cs_affiliate_1",
                  "amount_total": 99,
                  "customer_email": "monetization@whispr.com",
                  "metadata": {
                    "packId": "COINS_50",
                    "coins": "50",
                    "userEmail": "monetization@whispr.com",
                    "referrerId": "%s"
                  }
                }
              }
            }
            """, referrerId);

        monetizationService.handleWebhook(payload, null);

        // Commission de 30% sur 0.99 EUR = 0.30 EUR
        assertEquals(new BigDecimal("0.30"), referrerWallet.getEarningsEur());
        assertEquals(1, referrerWallet.getReferralCount());
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // 1 pour l'acheteur, 1 pour la commission
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

    @Test
    void createTipCheckoutSession_ShouldSavePendingMessage_AndReturnTipResponse() {
        Link mockLink = Link.builder()
                .id(UUID.randomUUID())
                .slug("creatorSlug")
                .isActive(true)
                .user(mockUser)
                .build();
        when(linkRepository.findBySlug("creatorSlug")).thenReturn(Optional.of(mockLink));
        when(clientMetadataResolver.resolve(any())).thenReturn(new ClientMetadata("hIp", "1.2.3.4", "UA", "France", "Paris", "Instagram"));
        when(aiSentimentService.analyzeSentiment(any())).thenReturn("POSITIVE");
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        CreateTipCheckoutRequest request = new CreateTipCheckoutRequest(
                "creatorSlug",
                "Super message secret !",
                "text",
                new BigDecimal("2.50"),
                null,
                null,
                null
        );

        TipCheckoutResponse response = monetizationService.createTipCheckoutSession(request, mock(jakarta.servlet.http.HttpServletRequest.class));

        assertNotNull(response);
        assertNotNull(response.sessionId());
        assertNotNull(response.checkoutUrl());
        assertNotNull(response.messageId());
        verify(messageRepository, atLeastOnce()).save(any(Message.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void simulateTipPayment_ShouldCreditCreator70Percent_AndMarkTipPaid() {
        Link mockLink = Link.builder()
                .id(UUID.randomUUID())
                .slug("creatorSlug")
                .isActive(true)
                .user(mockUser)
                .build();

        UUID messageId = UUID.randomUUID();
        Message tipMsg = Message.builder()
                .id(messageId)
                .link(mockLink)
                .content("Mon pourboire")
                .status("PENDING_TIP")
                .isTipPaid(false)
                .tipAmountEur(new BigDecimal("10.00"))
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(tipMsg));
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockWallet));

        monetizationService.simulateTipPayment(messageId);

        assertTrue(tipMsg.isTipPaid());
        assertEquals("UNREAD", tipMsg.getStatus());
        assertEquals(new BigDecimal("7.00"), mockWallet.getEarningsEur());
        verify(walletRepository, times(1)).save(mockWallet);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }


    @Test
    void createSuperWhisprCheckoutSession_ShouldSavePendingMessage_AndReturnSuperWhisprResponse() {
        Link mockLink = Link.builder()
                .id(UUID.randomUUID())
                .slug("creatorSlug")
                .isActive(true)
                .user(mockUser)
                .build();

        when(linkRepository.findBySlug("creatorSlug")).thenReturn(Optional.of(mockLink));
        when(clientMetadataResolver.resolve(any())).thenReturn(
                new ClientMetadata("hIp", "rIp", "Chrome/Win", "France", "Paris", "https://instagram.com")
        );
        when(aiSentimentService.analyzeSentiment(any())).thenReturn("POSITIVE");
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        CreateSuperWhisprCheckoutRequest request = new CreateSuperWhisprCheckoutRequest(
                "creatorSlug",
                "Super Whispr secret !",
                null,
                null,
                null
        );

        SuperWhisprCheckoutResponse response = monetizationService.createSuperWhisprCheckoutSession(request, mock(jakarta.servlet.http.HttpServletRequest.class));

        assertNotNull(response);
        assertNotNull(response.sessionId());
        assertNotNull(response.checkoutUrl());
        assertNotNull(response.messageId());
        verify(messageRepository, atLeastOnce()).save(any(Message.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void simulateSuperWhisprPayment_ShouldCreditCreatorAndMarkUnread() {
        Link mockLink = Link.builder()
                .id(UUID.randomUUID())
                .slug("creatorSlug")
                .isActive(true)
                .user(mockUser)
                .build();

        UUID messageId = UUID.randomUUID();
        Message swMsg = Message.builder()
                .id(messageId)
                .link(mockLink)
                .content("Mon Super Whispr")
                .type("SUPER_WHISPR")
                .status("PENDING_PAYMENT")
                .build();

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(swMsg));
        when(walletRepository.findByUserId(mockUser.getId())).thenReturn(Optional.of(mockWallet));

        monetizationService.simulateSuperWhisprPayment(messageId);

        assertEquals("UNREAD", swMsg.getStatus());
        assertEquals("SUPER_WHISPR", swMsg.getType());
        assertEquals(new BigDecimal("0.50"), mockWallet.getEarningsEur());
        verify(walletRepository, times(1)).save(mockWallet);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

}
