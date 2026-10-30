package com.whispr.backend.service;

import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.Message;
import com.whispr.backend.domain.Transaction;
import com.whispr.backend.domain.UnlockedClue;
import com.whispr.backend.domain.User;
import com.whispr.backend.domain.Wallet;
import com.whispr.backend.dto.ClueResponseDto;
import com.whispr.backend.dto.UnlockClueRequest;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.repository.MessageRepository;
import com.whispr.backend.repository.TransactionRepository;
import com.whispr.backend.repository.UnlockedClueRepository;
import com.whispr.backend.repository.UserRepository;
import com.whispr.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClueService {

    private final MonetizationService monetizationService;
    private final MessageRepository messageRepository;
    private final AuditLogRepository auditLogRepository;
    private final UnlockedClueRepository unlockedClueRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public static final int CLUE_COST = 15;

    @Transactional(readOnly = true)
    public List<ClueResponseDto> getCluesForMessage(String email, UUID messageId) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getLink().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        Wallet wallet = monetizationService.getOrCreateWallet(user);
        boolean isProUser = wallet.isPro() && (wallet.getProExpiresAt() == null || wallet.getProExpiresAt().isAfter(ZonedDateTime.now()));

        AuditLog audit = auditLogRepository.findByMessageId(messageId).orElse(null);
        Map<String, String> rawClues = generateRawClues(audit);

        List<UnlockedClue> unlockedList = unlockedClueRepository.findByUserIdAndMessageId(user.getId(), messageId);
        Set<String> unlockedTypes = new HashSet<>();
        for (UnlockedClue u : unlockedList) {
            unlockedTypes.add(u.getClueType());
        }

        List<ClueResponseDto> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : rawClues.entrySet()) {
            String type = entry.getKey();
            String val = entry.getValue();
            boolean isUnlocked = isProUser || unlockedTypes.contains(type);
            result.add(new ClueResponseDto(type, isUnlocked ? val : "VERROUILLÉ", isUnlocked));
        }

        return result;
    }

    @Transactional
    public ClueResponseDto unlockClue(String email, UnlockClueRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Message message = messageRepository.findById(request.messageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getLink().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        Wallet wallet = monetizationService.getOrCreateWallet(user);
        boolean isProUser = wallet.isPro() && (wallet.getProExpiresAt() == null || wallet.getProExpiresAt().isAfter(ZonedDateTime.now()));

        Optional<UnlockedClue> alreadyUnlocked = unlockedClueRepository
                .findByUserIdAndMessageIdAndClueType(user.getId(), request.messageId(), request.clueType());

        AuditLog audit = auditLogRepository.findByMessageId(request.messageId()).orElse(null);
        Map<String, String> rawClues = generateRawClues(audit);
        String clueValue = rawClues.getOrDefault(request.clueType(), "Non disponible");

        if (alreadyUnlocked.isPresent() || isProUser) {
            return new ClueResponseDto(request.clueType(), clueValue, true);
        }

        if (wallet.getCoins() < CLUE_COST) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Solde de pièces insuffisant (15 pièces requises).");
        }

        wallet.setCoins(wallet.getCoins() - CLUE_COST);
        walletRepository.save(wallet);

        UnlockedClue newUnlocked = UnlockedClue.builder()
                .user(user)
                .message(message)
                .clueType(request.clueType())
                .clueValue(clueValue)
                .build();
        unlockedClueRepository.save(newUnlocked);

        Transaction transaction = Transaction.builder()
                .user(user)
                .type("CLUE_UNLOCK")
                .amountEur(BigDecimal.ZERO)
                .coinsDelta(-CLUE_COST)
                .description("Déblocage d'indice: " + request.clueType())
                .build();
        transactionRepository.save(transaction);

        return new ClueResponseDto(request.clueType(), clueValue, true);
    }

    public Map<String, String> generateRawClues(AuditLog audit) {
        Map<String, String> clues = new LinkedHashMap<>();
        if (audit == null) {
            clues.put("LOCATION", "Région inconnue");
            clues.put("DEVICE", "Smartphone inconnu");
            clues.put("NETWORK", "Opérateur mobile / 4G");
            return clues;
        }

        // 1. Emplacement réel (Ville exacte et Pays)
        String city = audit.getCity() != null && !audit.getCity().isBlank() ? audit.getCity() : null;
        String country = audit.getCountry() != null && !audit.getCountry().isBlank() ? audit.getCountry() : "France";
        if (city != null) {
            clues.put("LOCATION", city + " (" + country + ")");
        } else {
            clues.put("LOCATION", country + " (Rayon < 15km)");
        }

        // 2. Modèle d'appareil réel
        String ua = audit.getUserAgent() != null ? audit.getUserAgent().toLowerCase() : "";
        if (ua.contains("iphone")) {
            clues.put("DEVICE", "Apple iPhone (iOS Safari)");
        } else if (ua.contains("ipad")) {
            clues.put("DEVICE", "Apple iPad (iOS Safari)");
        } else if (ua.contains("samsung")) {
            clues.put("DEVICE", "Samsung Galaxy (Android)");
        } else if (ua.contains("pixel")) {
            clues.put("DEVICE", "Google Pixel (Android)");
        } else if (ua.contains("android")) {
            clues.put("DEVICE", "Smartphone Android");
        } else if (ua.contains("macintosh") || ua.contains("mac os")) {
            clues.put("DEVICE", "MacBook / iMac (macOS)");
        } else if (ua.contains("windows")) {
            clues.put("DEVICE", "PC Portable / Fixe (Windows)");
        } else {
            clues.put("DEVICE", "Navigateur Web");
        }

        // 3. Réseau / Fournisseur estimé
        int hash = Math.abs(audit.getHashedIp() != null ? audit.getHashedIp().hashCode() : (int) System.currentTimeMillis());
        String[] operators = {"Orange Mobile (4G/5G)", "SFR Fibre / 5G", "Bouygues Telecom", "Free Mobile", "Wi-Fi Haut Débit"};
        clues.put("NETWORK", operators[hash % operators.length]);

        return clues;
    }

    @Transactional
    public List<ClueResponseDto> unlockAllCluesForMessage(User user, Message message) {
        AuditLog audit = auditLogRepository.findByMessageId(message.getId()).orElse(null);
        Map<String, String> rawClues = generateRawClues(audit);

        for (Map.Entry<String, String> entry : rawClues.entrySet()) {
            String type = entry.getKey();
            String val = entry.getValue();
            if (unlockedClueRepository.findByUserIdAndMessageIdAndClueType(user.getId(), message.getId(), type).isEmpty()) {
                UnlockedClue clue = UnlockedClue.builder()
                        .user(user)
                        .message(message)
                        .clueType(type)
                        .clueValue(val)
                        .build();
                unlockedClueRepository.save(clue);
            }
        }

        return getCluesForMessage(user.getEmail(), message.getId());
    }

}
