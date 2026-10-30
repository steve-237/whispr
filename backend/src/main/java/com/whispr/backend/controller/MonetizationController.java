package com.whispr.backend.controller;

import jakarta.servlet.http.HttpServletRequest;

import com.whispr.backend.dto.*;
import com.whispr.backend.service.ClueService;
import com.whispr.backend.service.MonetizationService;
import com.whispr.backend.service.PayoutService;
import com.whispr.backend.service.VisitorAnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/monetization")
@RequiredArgsConstructor
public class MonetizationController {

    private final MonetizationService monetizationService;
    private final ClueService clueService;
    private final VisitorAnalyticsService visitorAnalyticsService;
    private final PayoutService payoutService;

    @GetMapping("/wallet")
    public ResponseEntity<WalletDto> getWallet(Authentication authentication) {
        return ResponseEntity.ok(monetizationService.getWalletDto(authentication.getName()));
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            Authentication authentication,
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        return ResponseEntity.ok(monetizationService.createCheckoutSession(authentication.getName(), request));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        monetizationService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok("Webhook handled successfully");
    }

    
    @PostMapping("/create-tip-checkout")
    public ResponseEntity<TipCheckoutResponse> createTipCheckout(
            HttpServletRequest request,
            @Valid @RequestBody CreateTipCheckoutRequest tipRequest) {
        return ResponseEntity.ok(monetizationService.createTipCheckoutSession(tipRequest, request));
    }

    @PostMapping("/simulate-tip")
    public ResponseEntity<Void> simulateTipPayment(@RequestParam UUID messageId) {
        monetizationService.simulateTipPayment(messageId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create-super-whispr-checkout")
    public ResponseEntity<SuperWhisprCheckoutResponse> createSuperWhisprCheckout(
            HttpServletRequest request,
            @Valid @RequestBody CreateSuperWhisprCheckoutRequest superWhisprRequest) {
        return ResponseEntity.ok(monetizationService.createSuperWhisprCheckoutSession(superWhisprRequest, request));
    }

    @PostMapping("/simulate-super-whispr")
    public ResponseEntity<Void> simulateSuperWhispr(@RequestParam UUID messageId) {
        monetizationService.simulateSuperWhisprPayment(messageId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create-clue-checkout")
    public ResponseEntity<CheckoutSessionResponse> createExpressClueCheckout(
            Authentication authentication,
            @Valid @RequestBody CreateExpressClueCheckoutRequest request) {
        return ResponseEntity.ok(monetizationService.createExpressClueCheckoutSession(authentication.getName(), request.messageId()));
    }

    @PostMapping("/simulate-clue-unlock")
    public ResponseEntity<Void> simulateExpressClueUnlock(
            Authentication authentication,
            @RequestParam UUID messageId) {
        monetizationService.simulateExpressClueUnlock(authentication.getName(), messageId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/checkout-simulate")
    public ResponseEntity<WalletDto> simulateCheckout(
            Authentication authentication,
            @RequestBody CheckoutSimulationRequest request) {
        return ResponseEntity.ok(monetizationService.simulateCheckout(authentication.getName(), request));
    }

    @GetMapping("/clues/{messageId}")
    public ResponseEntity<List<ClueResponseDto>> getCluesForMessage(
            Authentication authentication,
            @PathVariable UUID messageId) {
        return ResponseEntity.ok(clueService.getCluesForMessage(authentication.getName(), messageId));
    }

    @PostMapping("/unlock-clue")
    public ResponseEntity<ClueResponseDto> unlockClue(
            Authentication authentication,
            @RequestBody UnlockClueRequest request) {
        return ResponseEntity.ok(clueService.unlockClue(authentication.getName(), request));
    }

    @GetMapping("/visitor-analytics")
    public ResponseEntity<VisitorAnalyticsDto> getVisitorAnalytics(Authentication authentication) {
        return ResponseEntity.ok(visitorAnalyticsService.getVisitorAnalytics(authentication.getName()));
    }

    @PostMapping("/payout-request")
    public ResponseEntity<PayoutResponseDto> createPayoutRequest(
            Authentication authentication,
            @RequestBody CreatePayoutRequest request) {
        return ResponseEntity.ok(payoutService.createPayoutRequest(authentication.getName(), request));
    }

    @GetMapping("/payout-history")
    public ResponseEntity<List<PayoutResponseDto>> getPayoutHistory(Authentication authentication) {
        return ResponseEntity.ok(payoutService.getUserPayoutHistory(authentication.getName()));
    }
}
