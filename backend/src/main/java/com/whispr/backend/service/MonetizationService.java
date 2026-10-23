package com.whispr.backend.service;

import com.whispr.backend.domain.Transaction;
import com.whispr.backend.domain.User;
import com.whispr.backend.domain.Wallet;
import com.whispr.backend.dto.CheckoutSimulationRequest;
import com.whispr.backend.dto.WalletDto;
import com.whispr.backend.repository.TransactionRepository;
import com.whispr.backend.repository.UserRepository;
import com.whispr.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class MonetizationService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public static final int WELCOME_COINS = 30;

    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .user(user)
                            .coins(WELCOME_COINS)
                            .totalSpentEur(BigDecimal.ZERO)
                            .isPro(false)
                            .earningsEur(BigDecimal.ZERO)
                            .referralCount(0)
                            .affiliateCode(user.getPseudo())
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    @Transactional(readOnly = true)
    public WalletDto getWalletDto(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = getOrCreateWallet(user);
        String affiliateCode = wallet.getAffiliateCode() != null ? wallet.getAffiliateCode() : user.getPseudo();
        return new WalletDto(
                wallet.getCoins(),
                wallet.getTotalSpentEur(),
                wallet.isPro(),
                wallet.getProExpiresAt(),
                wallet.getEarningsEur(),
                wallet.getReferralCount(),
                affiliateCode
        );
    }

    @Transactional
    public WalletDto simulateCheckout(String email, CheckoutSimulationRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Wallet wallet = getOrCreateWallet(user);

        BigDecimal amountEur;
        int coinsToAdd;
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
                coinsToAdd = 50;
                desc = "Abonnement Whispr PRO Club (1 mois)";
                type = "PRO_SUBSCRIPTION";
                wallet.setPro(true);
                wallet.setProExpiresAt(ZonedDateTime.now().plusMonths(1));
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pack inconnu: " + request.packId());
        }

        wallet.setCoins(wallet.getCoins() + coinsToAdd);
        wallet.setTotalSpentEur(wallet.getTotalSpentEur().add(amountEur));
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .user(user)
                .type(type)
                .amountEur(amountEur)
                .coinsDelta(coinsToAdd)
                .description(desc)
                .build();
        transactionRepository.save(transaction);

        String affiliateCode = wallet.getAffiliateCode() != null ? wallet.getAffiliateCode() : user.getPseudo();
        return new WalletDto(
                wallet.getCoins(),
                wallet.getTotalSpentEur(),
                wallet.isPro(),
                wallet.getProExpiresAt(),
                wallet.getEarningsEur(),
                wallet.getReferralCount(),
                affiliateCode
        );
    }
}
