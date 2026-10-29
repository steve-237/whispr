package com.whispr.backend.controller;

import com.whispr.backend.dto.AdvertisementDto;
import com.whispr.backend.dto.PayoutResponseDto;
import com.whispr.backend.dto.UpdatePayoutStatusRequest;
import com.whispr.backend.dto.admin.AdminAuditLogDto;
import com.whispr.backend.dto.admin.AdminMessageDto;
import com.whispr.backend.dto.admin.AdminUserDto;
import com.whispr.backend.dto.admin.CreateAdRequest;
import com.whispr.backend.service.AdminService;
import com.whispr.backend.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PayoutService payoutService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        return ResponseEntity.ok(adminService.getUsers(pageable).getContent());
    }

    @DeleteMapping("/users/{pseudo}")
    public ResponseEntity<Void> deleteUser(@PathVariable String pseudo) {
        adminService.deleteUser(pseudo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/bulk")
    public ResponseEntity<Void> deleteUsersBulk(@RequestBody List<String> pseudonyms) {
        adminService.deleteUsersBulk(pseudonyms);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/messages")
    public ResponseEntity<List<AdminMessageDto>> getMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        return ResponseEntity.ok(adminService.getMessages(pageable).getContent());
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID id) {
        adminService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages/bulk")
    public ResponseEntity<Void> deleteMessagesBulk(@RequestBody List<UUID> ids) {
        adminService.deleteMessagesBulk(ids);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{pseudo}/ban")
    public ResponseEntity<Void> banUser(@PathVariable String pseudo) {
        adminService.banUser(pseudo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{pseudo}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable String pseudo) {
        adminService.unbanUser(pseudo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{pseudo}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable String pseudo) {
        return adminService.resetPassword(pseudo)
                .map(pwd -> ResponseEntity.ok(Map.of("newPassword", pwd)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AdminAuditLogDto>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Principal principal) {
        boolean isSuperAdmin = principal != null && "admin@whispr.com".equalsIgnoreCase(principal.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        return ResponseEntity.ok(adminService.getAuditLogs(pageable, isSuperAdmin).getContent());
    }

    @GetMapping("/ads")
    public ResponseEntity<List<AdvertisementDto>> getAdminAds() {
        return ResponseEntity.ok(adminService.getAdminAds());
    }

    @PostMapping("/ads")
    public ResponseEntity<AdvertisementDto> createAd(@RequestBody CreateAdRequest req) {
        return ResponseEntity.ok(adminService.createAd(req));
    }

    @PutMapping("/ads/{id}/toggle")
    public ResponseEntity<Void> toggleAd(@PathVariable UUID id) {
        adminService.toggleAd(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/ads/{id}")
    public ResponseEntity<Void> deleteAd(@PathVariable UUID id) {
        adminService.deleteAd(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/payouts")
    public ResponseEntity<List<PayoutResponseDto>> getPayouts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(payoutService.getAllPayoutRequests(pageable).getContent());
    }

    @PutMapping("/payouts/{id}/status")
    public ResponseEntity<PayoutResponseDto> updatePayoutStatus(
            @PathVariable UUID id,
            @RequestBody UpdatePayoutStatusRequest request) {
        return ResponseEntity.ok(payoutService.updatePayoutStatus(id, request));
    }
}
