package com.whispr.backend.controller;

import com.whispr.backend.dto.*;
import com.whispr.backend.service.MonetizationService;
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

    @GetMapping("/wallet")
    public ResponseEntity<WalletDto> getWallet(Authentication authentication) {
        return ResponseEntity.ok(monetizationService.getWalletDto(authentication.getName()));
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
        return ResponseEntity.ok(monetizationService.getCluesForMessage(authentication.getName(), messageId));
    }

    @PostMapping("/unlock-clue")
    public ResponseEntity<ClueResponseDto> unlockClue(
            Authentication authentication,
            @RequestBody UnlockClueRequest request) {
        return ResponseEntity.ok(monetizationService.unlockClue(authentication.getName(), request));
    }

    @GetMapping("/visitor-analytics")
    public ResponseEntity<VisitorAnalyticsDto> getVisitorAnalytics(Authentication authentication) {
        return ResponseEntity.ok(monetizationService.getVisitorAnalytics(authentication.getName()));
    }
}
