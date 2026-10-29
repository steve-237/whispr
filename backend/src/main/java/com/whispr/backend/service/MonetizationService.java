package com.whispr.backend.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonetizationService {

    private static final Logger log = LoggerFactory.getLogger(MonetizationService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Value("${whispr.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${whispr.stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Value("${whispr.frontend-url:http://localhost:4200}")
    private String frontendUrl;

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

    /**
     * Initialise une session Stripe Checkout pour l'achat d'un pack de pièces ou un abonnement PRO.
     */
    @Transactional
    public CheckoutSessionResponse createCheckoutSession(String email, CreateCheckoutSessionRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        long baseAmountCents;
        int coinsToAdd;
        String packTitle;
        String packDesc;

        switch (request.packId()) {
            case "COINS_50" -> {
                baseAmountCents = 99L; // 0.99 EUR
                coinsToAdd = 50;
                packTitle = "Whispr Pack Starter (50 Coins)";
                packDesc = "Débloquez les premiers indices sur l'expéditeur de vos messages";
            }
            case "COINS_150" -> {
                baseAmountCents = 249L; // 2.49 EUR
                coinsToAdd = 150;
                packTitle = "Whispr Pack Populaire (150 Coins)";
                packDesc = "Révélez des indices complets et profitez des fonctionnalités avancées";
            }
            case "COINS_500" -> {
                baseAmountCents = 599L; // 5.99 EUR
                coinsToAdd = 500;
                packTitle = "Whispr Pack VIP (500 Coins)";
                packDesc = "Accès maximal aux révélations d'indices et super whispers";
            }
            case "PRO_MONTHLY" -> {
                baseAmountCents = 499L; // 4.99 EUR
                coinsToAdd = 50;
                packTitle = "Abonnement Whispr PRO Club (1 mois)";
                packDesc = "Badge PRO vérifié, 50 coins offerts et fonctionnalités exclusives";
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pack inconnu: " + request.packId());
        }

        // Vérification du code promo parrain / influenceur
        User referrer = null;
        long finalAmountCents = baseAmountCents;
        String promoCode = request.promoCode();
        if (promoCode != null && !promoCode.isBlank()) {
            promoCode = promoCode.trim().toUpperCase();
            String finalPromo = promoCode;
            referrer = walletRepository.findByAffiliateCodeIgnoreCase(finalPromo)
                    .map(Wallet::getUser)
                    .or(() -> userRepository.findByPseudoIgnoreCase(finalPromo))
                    .orElse(null);

            // Ne pas permettre d'être son propre parrain
            if (referrer != null && referrer.getId().equals(user.getId())) {
                referrer = null;
            }

            if (referrer != null) {
                // Réduction de 10%
                finalAmountCents = Math.round(baseAmountCents * 0.90);
                log.info("Code promo {} appliqué pour {}: -10% ({} cents)", finalPromo, user.getPseudo(), finalAmountCents);
            }
        }

        // Mode réel Stripe si clé secrète configurée
        if (stripeSecretKey != null && !stripeSecretKey.isBlank() && !stripeSecretKey.startsWith("mock")) {
            Stripe.apiKey = stripeSecretKey;

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/inbox?payment=success&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/inbox?payment=cancelled")
                    .setCustomerEmail(user.getEmail())
                    .putMetadata("userId", user.getId().toString())
                    .putMetadata("userEmail", user.getEmail())
                    .putMetadata("packId", request.packId())
                    .putMetadata("coins", String.valueOf(coinsToAdd))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(finalAmountCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(packTitle)
                                                                    .setDescription(packDesc)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    );

            if (referrer != null) {
                paramsBuilder.putMetadata("promoCode", promoCode);
                paramsBuilder.putMetadata("referrerId", referrer.getId().toString());
            }

            try {
                Session session = Session.create(paramsBuilder.build());
                return new CheckoutSessionResponse(session.getId(), session.getUrl());
            } catch (StripeException e) {
                log.error("Erreur création session Stripe: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la création de la session Stripe: " + e.getMessage());
            }
        }

        // Fallback simulation / mode développement
        String mockSessionId = "mock_cs_" + UUID.randomUUID();
        String mockCheckoutUrl = frontendUrl + "/inbox?payment=success&session_id=" + mockSessionId;
        log.info("Stripe non configuré, retour session simulée: {}", mockSessionId);
        return new CheckoutSessionResponse(mockSessionId, mockCheckoutUrl);
    }

    /**
     * Traite les événements de webhook Stripe avec validation de signature et idempotence.
     */
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        if (stripeWebhookSecret != null && !stripeWebhookSecret.isBlank() && sigHeader != null && !sigHeader.isBlank()) {
            try {
                event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
            } catch (SignatureVerificationException e) {
                log.error("Signature Stripe invalide: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signature Stripe invalide");
            }
        } else {
            // Désérialisation directe si pas de secret (tests unitaires ou mock)
            try {
                event = Event.GSON.fromJson(payload, Event.class);
            } catch (Exception e) {
                log.error("Erreur parsing payload Stripe: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload JSON invalide");
            }
        }

        if (event == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event Stripe null");
        }

        log.info("Traitement événement Stripe: id={}, type={}", event.getId(), event.getType());

        if ("checkout.session.completed".equals(event.getType())) {
            processCheckoutSessionCompleted(event);
        } else if ("payment_intent.payment_failed".equals(event.getType()) || "checkout.session.expired".equals(event.getType())) {
            log.warn("Paiement Stripe annulé ou expiré: id={}, type={}", event.getId(), event.getType());
        }
    }

    private void processCheckoutSessionCompleted(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        Session session = null;
        try {
            if (dataObjectDeserializer.getObject().isPresent()) {
                StripeObject obj = dataObjectDeserializer.getObject().get();
                if (obj instanceof Session s) {
                    session = s;
                }
            }
        } catch (Exception ignored) {}

        if (session == null) {
            try {
                StripeObject obj = dataObjectDeserializer.deserializeUnsafe();
                if (obj instanceof Session s) {
                    session = s;
                }
            } catch (Exception ignored) {}
        }

        if (session == null && dataObjectDeserializer.getRawJson() != null) {
            try {
                session = Event.GSON.fromJson(dataObjectDeserializer.getRawJson(), Session.class);
            } catch (Exception ignored) {}
        }

        if (session == null) {
            log.warn("Impossible d'extraire la session Stripe de l'événement {}", event.getId());
            return;
        }

        // Idempotence : vérifier si l'événement ou la session a déjà été traité(e)
        if (transactionRepository.existsByStripeEventId(event.getId()) ||
                (session.getId() != null && transactionRepository.existsByStripeSessionId(session.getId()))) {
            log.info("Transaction Stripe déjà traitée pour session={} event={}", session.getId(), event.getId());
            return;
        }

        Map<String, String> metadata = session.getMetadata();
        String userIdStr = metadata != null ? metadata.get("userId") : null;
        String userEmail = metadata != null ? metadata.get("userEmail") : session.getCustomerEmail();
        String packId = metadata != null ? metadata.get("packId") : "COINS_50";
        String coinsStr = metadata != null ? metadata.get("coins") : "50";
        String referrerIdStr = metadata != null ? metadata.get("referrerId") : null;

        User user = null;
        if (userIdStr != null && !userIdStr.isBlank()) {
            try {
                user = userRepository.findById(UUID.fromString(userIdStr)).orElse(null);
            } catch (Exception ignored) {}
        }
        if (user == null && userEmail != null) {
            user = userRepository.findByEmailIgnoreCase(userEmail).orElse(null);
        }

        if (user == null) {
            log.error("Utilisateur introuvable pour la session Stripe {}", session.getId());
            return;
        }

        final User buyer = user;

        Wallet wallet = getOrCreateWallet(user);
        int coinsToAdd = 50;
        try {
            coinsToAdd = Integer.parseInt(coinsStr);
        } catch (NumberFormatException ignored) {}

        BigDecimal amountEur;
        if (session.getAmountTotal() != null) {
            amountEur = BigDecimal.valueOf(session.getAmountTotal()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            amountEur = getPackDefaultPrice(packId);
        }

        wallet.setCoins(wallet.getCoins() + coinsToAdd);
        wallet.setTotalSpentEur(wallet.getTotalSpentEur().add(amountEur));

        if ("PRO_MONTHLY".equalsIgnoreCase(packId)) {
            wallet.setPro(true);
            wallet.setProExpiresAt(ZonedDateTime.now().plusMonths(1));
        }
        walletRepository.save(wallet);

        // Commission parrain (30%)
        if (referrerIdStr != null && !referrerIdStr.isBlank()) {
            try {
                UUID referrerId = UUID.fromString(referrerIdStr);
                userRepository.findById(referrerId).ifPresent(refUser -> {
                    Wallet refWallet = getOrCreateWallet(refUser);
                    BigDecimal commission = amountEur.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);
                    refWallet.setEarningsEur(refWallet.getEarningsEur().add(commission));
                    refWallet.setReferralCount(refWallet.getReferralCount() + 1);
                    walletRepository.save(refWallet);

                    Transaction refTx = Transaction.builder()
                            .user(refUser)
                            .type("AFFILIATE_COMMISSION")
                            .amountEur(commission)
                            .coinsDelta(0)
                            .description("Commission affiliation (30%) - Achat par @" + buyer.getPseudo())
                            .stripeEventId(event.getId() + "_ref")
                            .status("COMPLETED")
                            .build();
                    transactionRepository.save(refTx);
                    log.info("Commission parrain {} EUR créditée à {}", commission, refUser.getPseudo());
                });
            } catch (Exception e) {
                log.warn("Erreur commission parrain: {}", e.getMessage());
            }
        }

        // Enregistrement de la transaction d'achat utilisateur
        Transaction transaction = Transaction.builder()
                .user(user)
                .type("PRO_MONTHLY".equalsIgnoreCase(packId) ? "PRO_SUBSCRIPTION" : "COIN_PURCHASE")
                .amountEur(amountEur)
                .coinsDelta(coinsToAdd)
                .description("Achat Stripe: " + getPackTitle(packId))
                .stripeSessionId(session.getId())
                .stripeEventId(event.getId())
                .status("COMPLETED")
                .build();
        transactionRepository.save(transaction);

        log.info("Transaction Stripe auditée et validée: user={}, montant={} EUR, pièces={}, eventId={}",
                user.getEmail(), amountEur, coinsToAdd, event.getId());
    }

    private BigDecimal getPackDefaultPrice(String packId) {
        return switch (packId) {
            case "COINS_150" -> new BigDecimal("2.49");
            case "COINS_500" -> new BigDecimal("5.99");
            case "PRO_MONTHLY" -> new BigDecimal("4.99");
            default -> new BigDecimal("0.99");
        };
    }

    private String getPackTitle(String packId) {
        return switch (packId) {
            case "COINS_150" -> "Pack Populaire (150 Coins)";
            case "COINS_500" -> "Pack VIP (500 Coins)";
            case "PRO_MONTHLY" -> "Abonnement PRO Club (1 mois)";
            default -> "Pack Starter (50 Coins)";
        };
    }

    /**
     * Méthode de simulation pour développement local et tests sans Stripe.
     */
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
                .status("COMPLETED")
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
