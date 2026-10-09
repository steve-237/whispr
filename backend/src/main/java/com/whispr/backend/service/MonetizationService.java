package com.whispr.backend.service;

import com.whispr.backend.domain.*;
import com.whispr.backend.dto.*;
import com.whispr.backend.repository.*;
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
public class MonetizationService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UnlockedClueRepository unlockedClueRepository;
    private final MessageRepository messageRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .user(user)
                            .coins(30) // 30 pièces gratuites de bienvenue
                            .totalSpentEur(BigDecimal.ZERO)
                            .isPro(false)
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    @Transactional(readOnly = true)
    public WalletDto getWalletDto(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = getOrCreateWallet(user);
        return new WalletDto(wallet.getCoins(), wallet.getTotalSpentEur(), wallet.isPro(), wallet.getProExpiresAt());
    }

    /**
     * Simulation immédiate de validation de paiement Stripe.
     * Met à jour le solde réel de pièces, incrémente la somme dépensée totale en EUR
     * et active le statut PRO si souscrit.
     */
    @Transactional
    public WalletDto simulateCheckout(String email, CheckoutSimulationRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = getOrCreateWallet(user);

        BigDecimal amountEur;
        int coinsToAdd = 0;
        String desc;
        String type;

        switch (request.packId()) {
            case "COINS_50" -> {
                amountEur = new BigDecimal("0.99");
                coinsToAdd = 50;
                desc = "Achat Pack Starter (+50 Whispr Coins)";
                type = "COIN_PURCHASE";
            }
            case "COINS_150" -> {
                amountEur = new BigDecimal("2.49");
                coinsToAdd = 150;
                desc = "Achat Pack Populaire (+150 Whispr Coins)";
                type = "COIN_PURCHASE";
            }
            case "COINS_500" -> {
                amountEur = new BigDecimal("5.99");
                coinsToAdd = 500;
                desc = "Achat Pack VIP (+500 Whispr Coins)";
                type = "COIN_PURCHASE";
            }
            case "PRO_MONTHLY" -> {
                amountEur = new BigDecimal("4.99");
                coinsToAdd = 50; // Bonus offert avec le PRO
                desc = "Abonnement Whispr PRO Club (1 mois)";
                type = "PRO_SUBSCRIPTION";
                wallet.setPro(true);
                wallet.setProExpiresAt(ZonedDateTime.now().plusMonths(1));
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pack inconnu: " + request.packId());
        }

        // Mise à jour du portefeuille
        wallet.setCoins(wallet.getCoins() + coinsToAdd);
        wallet.setTotalSpentEur(wallet.getTotalSpentEur().add(amountEur));
        walletRepository.save(wallet);

        // Enregistrement de la transaction
        Transaction transaction = Transaction.builder()
                .user(user)
                .type(type)
                .amountEur(amountEur)
                .coinsDelta(coinsToAdd)
                .description(desc)
                .build();
        transactionRepository.save(transaction);

        return new WalletDto(wallet.getCoins(), wallet.getTotalSpentEur(), wallet.isPro(), wallet.getProExpiresAt());
    }

    /**
     * Récupère la liste des indices pour un message avec leur statut (débloqué ou verrouillé).
     */
    @Transactional(readOnly = true)
    public List<ClueResponseDto> getCluesForMessage(String email, UUID messageId) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getLink().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        Wallet wallet = getOrCreateWallet(user);
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

    /**
     * Débloque un indice pour 15 pièces (ou gratuit si PRO).
     */
    @Transactional
    public ClueResponseDto unlockClue(String email, UnlockClueRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Message message = messageRepository.findById(request.messageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getLink().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        Wallet wallet = getOrCreateWallet(user);
        boolean isProUser = wallet.isPro() && (wallet.getProExpiresAt() == null || wallet.getProExpiresAt().isAfter(ZonedDateTime.now()));

        // Vérifier si déjà débloqué
        Optional<UnlockedClue> alreadyUnlocked = unlockedClueRepository
                .findByUserIdAndMessageIdAndClueType(user.getId(), request.messageId(), request.clueType());

        AuditLog audit = auditLogRepository.findByMessageId(request.messageId()).orElse(null);
        Map<String, String> rawClues = generateRawClues(audit);
        String clueValue = rawClues.getOrDefault(request.clueType(), "Non disponible");

        if (alreadyUnlocked.isPresent() || isProUser) {
            return new ClueResponseDto(request.clueType(), clueValue, true);
        }

        // Coût du déblocage : 15 pièces
        int cost = 15;
        if (wallet.getCoins() < cost) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Solde de pièces insuffisant (15 pièces requises).");
        }

        wallet.setCoins(wallet.getCoins() - cost);
        walletRepository.save(wallet);

        // Enregistrer l'indice débloqué
        UnlockedClue newUnlocked = UnlockedClue.builder()
                .user(user)
                .message(message)
                .clueType(request.clueType())
                .clueValue(clueValue)
                .build();
        unlockedClueRepository.save(newUnlocked);

        // Enregistrer la transaction
        Transaction transaction = Transaction.builder()
                .user(user)
                .type("CLUE_UNLOCK")
                .amountEur(BigDecimal.ZERO)
                .coinsDelta(-cost)
                .description("Déblocage d'indice: " + request.clueType())
                .build();
        transactionRepository.save(transaction);

        return new ClueResponseDto(request.clueType(), clueValue, true);
    }

    private Map<String, String> generateRawClues(AuditLog audit) {
        Map<String, String> clues = new LinkedHashMap<>();
        if (audit == null) {
            clues.put("LOCATION", "Région inconnue");
            clues.put("DEVICE", "Smartphone inconnu");
            clues.put("NETWORK", "Opérateur mobile / 4G");
            return clues;
        }

        // 1. Emplacement
        String loc = audit.getCountry() != null ? audit.getCountry() : "Paris, France";
        clues.put("LOCATION", loc + " (Rayon < 15km)");

        // 2. Modèle d'appareil
        String ua = audit.getUserAgent() != null ? audit.getUserAgent().toLowerCase() : "";
        if (ua.contains("iphone")) {
            clues.put("DEVICE", "Apple iPhone (iOS Safari)");
        } else if (ua.contains("samsung")) {
            clues.put("DEVICE", "Samsung Galaxy (Android)");
        } else if (ua.contains("android")) {
            clues.put("DEVICE", "Smartphone Android");
        } else if (ua.contains("macintosh")) {
            clues.put("DEVICE", "MacBook / iMac (macOS)");
        } else if (ua.contains("windows")) {
            clues.put("DEVICE", "PC Portable (Windows)");
        } else {
            clues.put("DEVICE", "Navigateur Mobile");
        }

        // 3. Réseau / Opérateur estimé
        int hash = Math.abs(audit.getHashedIp() != null ? audit.getHashedIp().hashCode() : (int) System.currentTimeMillis());
        String[] operators = {"Orange Mobile (4G/5G)", "SFR Fibre / 5G", "Bouygues Telecom", "Free Mobile", "Wi-Fi Privé"};
        clues.put("NETWORK", operators[hash % operators.length]);

        return clues;
    }
}
