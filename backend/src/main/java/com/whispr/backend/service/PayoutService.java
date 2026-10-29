package com.whispr.backend.service;

import com.whispr.backend.domain.PayoutRequest;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutService {

    public static final BigDecimal MIN_PAYOUT_AMOUNT = new BigDecimal("20.00");

    private final PayoutRequestRepository payoutRequestRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public PayoutResponseDto createPayoutRequest(String userEmail, CreatePayoutRequest req) {
        if (req == null || req.amountEur() == null || req.amountEur().compareTo(MIN_PAYOUT_AMOUNT) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le montant minimum de retrait est de 20,00 €.");
        }
        if (req.method() == null || req.method().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La méthode de paiement est requise (PAYPAL ou IBAN).");
        }
        if (req.payoutDetails() == null || req.payoutDetails().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les coordonnées de paiement sont requises.");
        }

        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portefeuille introuvable"));

        if (wallet.getEarningsEur().compareTo(req.amountEur()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solde de gains insuffisant (" + wallet.getEarningsEur() + " € disponible).");
        }

        // Déduction immédiate des gains du portefeuille
        wallet.setEarningsEur(wallet.getEarningsEur().subtract(req.amountEur()));
        walletRepository.save(wallet);

        // Transaction d'historique
        Transaction tx = Transaction.builder()
                .user(user)
                .type("PAYOUT_REQUEST")
                .amountEur(req.amountEur().negate())
                .coinsDelta(0)
                .description("Demande de retrait " + req.method().toUpperCase() + " (" + req.amountEur() + " €)")
                .build();
        transactionRepository.save(tx);

        PayoutRequest payoutRequest = PayoutRequest.builder()
                .user(user)
                .amountEur(req.amountEur())
                .method(req.method().trim().toUpperCase())
                .payoutDetails(req.payoutDetails().trim())
                .status("PENDING")
                .build();

        PayoutRequest saved = payoutRequestRepository.save(payoutRequest);
        log.info("Nouvelle demande de retrait créée: ID={} user={} montant={} €", saved.getId(), user.getPseudo(), req.amountEur());
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PayoutResponseDto> getUserPayoutHistory(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        return payoutRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PayoutResponseDto> getAllPayoutRequests(Pageable pageable) {
        return payoutRequestRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public PayoutResponseDto updatePayoutStatus(UUID payoutId, UpdatePayoutStatusRequest req) {
        PayoutRequest payout = payoutRequestRepository.findById(payoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de retrait introuvable"));

        String oldStatus = payout.getStatus();
        String newStatus = req.status() != null ? req.status().trim().toUpperCase() : oldStatus;

        if (!List.of("PENDING", "APPROVED", "PAID", "REJECTED").contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut de retrait invalide : " + newStatus);
        }

        // Si la demande est rejetée et qu'elle n'était pas déjà rejetée, on recrédite les gains de l'utilisateur
        if ("REJECTED".equals(newStatus) && !"REJECTED".equals(oldStatus)) {
            Wallet wallet = walletRepository.findByUserId(payout.getUser().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portefeuille utilisateur introuvable"));
            wallet.setEarningsEur(wallet.getEarningsEur().add(payout.getAmountEur()));
            walletRepository.save(wallet);

            Transaction refundTx = Transaction.builder()
                    .user(payout.getUser())
                    .type("PAYOUT_REFUND")
                    .amountEur(payout.getAmountEur())
                    .coinsDelta(0)
                    .description("Remboursement suite à rejet de retrait (" + payout.getAmountEur() + " €)")
                    .build();
            transactionRepository.save(refundTx);
            log.info("Remboursement des gains suite au rejet du retrait ID={} user={} montant={} €", payout.getId(), payout.getUser().getPseudo(), payout.getAmountEur());
        }

        payout.setStatus(newStatus);
        if (req.adminNote() != null) {
            payout.setAdminNote(req.adminNote().trim());
        }

        PayoutRequest updated = payoutRequestRepository.save(payout);
        return mapToDto(updated);
    }

    private PayoutResponseDto mapToDto(PayoutRequest p) {
        return new PayoutResponseDto(
                p.getId(),
                p.getUser().getId(),
                p.getUser().getPseudo(),
                p.getUser().getEmail(),
                p.getAmountEur(),
                p.getMethod(),
                p.getPayoutDetails(),
                p.getStatus(),
                p.getAdminNote(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
