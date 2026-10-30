package com.whispr.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import com.whispr.backend.domain.AuditLog;
import com.whispr.backend.domain.Link;
import com.whispr.backend.domain.Message;
import com.whispr.backend.dto.CreateTipCheckoutRequest;
import com.whispr.backend.dto.TipCheckoutResponse;
import com.whispr.backend.dto.CreateSuperWhisprCheckoutRequest;
import com.whispr.backend.dto.SuperWhisprCheckoutResponse;
import com.whispr.backend.repository.AuditLogRepository;
import com.whispr.backend.domain.UnlockedClue;
import com.whispr.backend.repository.UnlockedClueRepository;
import java.util.LinkedHashMap;
import com.whispr.backend.repository.LinkRepository;
import com.whispr.backend.repository.MessageRepository;
import com.whispr.backend.util.ClientMetadata;
import com.whispr.backend.util.ClientMetadataResolver;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
    private final LinkRepository linkRepository;
    private final MessageRepository messageRepository;
    private final AuditLogRepository auditLogRepository;
    private final UnlockedClueRepository unlockedClueRepository;
    private final AiSentimentService aiSentimentService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ClientMetadataResolver clientMetadataResolver;


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
        String sessionType = metadata != null ? metadata.get("type") : null;
        if ("CREATOR_TIP".equalsIgnoreCase(sessionType)) {
            processCreatorTipCompleted(session, event);
            return;
        }
        if ("SUPER_WHISPR".equalsIgnoreCase(sessionType)) {
            processSuperWhisprCompleted(session, event);
            return;
        }
        if ("EXPRESS_CLUE_UNLOCK".equalsIgnoreCase(sessionType)) {
            processExpressClueUnlockCompleted(session, event);
            return;
        }

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

    @Transactional
    public TipCheckoutResponse createTipCheckoutSession(CreateTipCheckoutRequest request, HttpServletRequest servletRequest) {
        Link link = linkRepository.findBySlug(request.slug())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil introuvable pour @" + request.slug()));

        if (Boolean.FALSE.equals(link.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce lien Whispr est actuellement inactif.");
        }

        BigDecimal tipAmount = request.tipAmountEur();
        if (tipAmount == null || tipAmount.compareTo(BigDecimal.valueOf(0.50)) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le montant minimum de pourboire est de 0.50 €.");
        }

        ClientMetadata metadata = clientMetadataResolver.resolve(servletRequest);

        String content = request.content();
        String sentiment = aiSentimentService != null ? aiSentimentService.analyzeSentiment(content) : "NEUTRAL";

        String finalType;
        if ((request.audioUrl() != null && !request.audioUrl().isBlank()) || "AUDIO".equalsIgnoreCase(request.type())) {
            finalType = "AUDIO";
            if (content == null || content.isBlank()) {
                String filterLabel = (request.voiceFilter() != null) ? request.voiceFilter() : "NORMAL";
                content = "🎙️ Mémo vocal (" + filterLabel + ")";
            }
        } else {
            finalType = "text";
        }

        Message message = Message.builder()
                .link(link)
                .content(content)
                .type(finalType)
                .status("PENDING_TIP")
                .isTipPaid(false)
                .tipAmountEur(tipAmount)
                .aiCategory(sentiment)
                .audioUrl(request.audioUrl())
                .audioDurationSeconds(request.audioDurationSeconds())
                .voiceFilter(request.voiceFilter())
                .build();
        message = messageRepository.save(message);

        AuditLog auditLog = AuditLog.builder()
                .message(message)
                .hashedIp(metadata.hashedIp())
                .rawIp(metadata.rawIp())
                .userAgent(metadata.userAgent())
                .country(metadata.country())
                .city(metadata.city())
                .referrer(metadata.referrer())
                .build();
        auditLogRepository.save(auditLog);
        message.setAuditLog(auditLog);

        long amountCents = tipAmount.multiply(BigDecimal.valueOf(100)).longValue();
        User creator = link.getUser();

        if (stripeSecretKey != null && !stripeSecretKey.isBlank() && !stripeSecretKey.startsWith("mock")) {
            Stripe.apiKey = stripeSecretKey;
            try {
                SessionCreateParams params = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(frontendUrl + "/@" + link.getSlug() + "?payment=success&tip=true&amount=" + tipAmount)
                        .setCancelUrl(frontendUrl + "/@" + link.getSlug() + "?payment=cancelled")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("eur")
                                                        .setUnitAmount(amountCents)
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName("Pourboire Whispr pour @" + creator.getPseudo())
                                                                        .setDescription("Pourboire direct joint au message anonyme (70% reversés au créateur)")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .putMetadata("type", "CREATOR_TIP")
                        .putMetadata("messageId", message.getId().toString())
                        .putMetadata("creatorId", creator.getId().toString())
                        .putMetadata("creatorEmail", creator.getEmail())
                        .putMetadata("tipAmountEur", tipAmount.toString())
                        .build();

                Session session = Session.create(params);
                message.setStripeTipSessionId(session.getId());
                messageRepository.save(message);

                log.info("Session Stripe créée pour pourboire: session={} messageId={} montant={} EUR", session.getId(), message.getId(), tipAmount);
                return new TipCheckoutResponse(session.getId(), session.getUrl(), message.getId());
            } catch (StripeException e) {
                log.error("Erreur création session Stripe pourboire: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur Stripe: " + e.getMessage());
            }
        }

        String mockSessionId = "mock_tip_" + UUID.randomUUID();
        message.setStripeTipSessionId(mockSessionId);
        messageRepository.save(message);

        String mockCheckoutUrl = frontendUrl + "/@" + link.getSlug() + "?payment=success&tip=true&amount=" + tipAmount + "&mockTipId=" + message.getId();
        log.info("Stripe non configuré, retour session pourboire simulée: {}", mockSessionId);
        return new TipCheckoutResponse(mockSessionId, mockCheckoutUrl, message.getId());
    }

    @Transactional
    public void processCreatorTipCompleted(Session session, Event event) {
        Map<String, String> metadata = session.getMetadata();
        String messageIdStr = metadata != null ? metadata.get("messageId") : null;
        String creatorIdStr = metadata != null ? metadata.get("creatorId") : null;

        if (messageIdStr == null || messageIdStr.isBlank()) {
            log.error("messageId manquant pour la session pourboire Stripe {}", session.getId());
            return;
        }

        UUID messageId = UUID.fromString(messageIdStr);
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            log.error("Message introuvable pour le pourboire messageId={}", messageId);
            return;
        }

        if (message.isTipPaid()) {
            log.info("Pourboire déjà payé pour messageId={}", messageId);
            return;
        }

        message.setTipPaid(true);
        message.setStatus("UNREAD");
        messageRepository.save(message);

        User creator = null;
        if (creatorIdStr != null && !creatorIdStr.isBlank()) {
            creator = userRepository.findById(UUID.fromString(creatorIdStr)).orElse(null);
        }
        if (creator == null) {
            creator = message.getLink().getUser();
        }

        BigDecimal tipAmount = message.getTipAmountEur();
        BigDecimal creatorShare = tipAmount.multiply(BigDecimal.valueOf(0.70)).setScale(2, RoundingMode.HALF_UP);

        Wallet creatorWallet = getOrCreateWallet(creator);
        creatorWallet.setEarningsEur(creatorWallet.getEarningsEur().add(creatorShare));
        walletRepository.save(creatorWallet);

        Transaction tipTx = Transaction.builder()
                .user(creator)
                .type("CREATOR_TIP")
                .amountEur(creatorShare)
                .coinsDelta(0)
                .description("Pourboire reçu de " + tipAmount + " € (+ " + creatorShare + " € pour vous)")
                .stripeSessionId(session.getId())
                .stripeEventId(event != null ? event.getId() : "mock_evt_" + UUID.randomUUID())
                .status("COMPLETED")
                .build();
        transactionRepository.save(tipTx);

        log.info("Pourboire de {} EUR validé ! {} EUR crédités à {}", tipAmount, creatorShare, creator.getPseudo());

        if (messagingTemplate != null && messageService != null) {
            try {
                messagingTemplate.convertAndSend("/topic/user/" + creator.getEmail() + "/messages", messageService.toDto(message));
            } catch (Exception e) {
                log.warn("Impossible d'envoyer la notification WebSocket pour le pourboire: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public void simulateTipPayment(UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message introuvable"));

        if (message.isTipPaid()) return;

        message.setTipPaid(true);
        message.setStatus("UNREAD");
        messageRepository.save(message);

        User creator = message.getLink().getUser();
        BigDecimal tipAmount = message.getTipAmountEur() != null ? message.getTipAmountEur() : BigDecimal.valueOf(2.00);
        BigDecimal creatorShare = tipAmount.multiply(BigDecimal.valueOf(0.70)).setScale(2, RoundingMode.HALF_UP);

        Wallet creatorWallet = getOrCreateWallet(creator);
        creatorWallet.setEarningsEur(creatorWallet.getEarningsEur().add(creatorShare));
        walletRepository.save(creatorWallet);

        Transaction tipTx = Transaction.builder()
                .user(creator)
                .type("CREATOR_TIP")
                .amountEur(creatorShare)
                .coinsDelta(0)
                .description("Pourboire simulé de " + tipAmount + " € (+ " + creatorShare + " €)")
                .stripeSessionId(message.getStripeTipSessionId() != null ? message.getStripeTipSessionId() : "sim_" + UUID.randomUUID())
                .status("COMPLETED")
                .build();
        transactionRepository.save(tipTx);

        if (messagingTemplate != null && messageService != null) {
            try {
                messagingTemplate.convertAndSend("/topic/user/" + creator.getEmail() + "/messages", messageService.toDto(message));
            } catch (Exception ignored) {}
        }
    }


    @Transactional
    public SuperWhisprCheckoutResponse createSuperWhisprCheckoutSession(CreateSuperWhisprCheckoutRequest request, HttpServletRequest servletRequest) {
        Link link = linkRepository.findBySlug(request.slug())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil introuvable pour @" + request.slug()));

        if (Boolean.FALSE.equals(link.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce lien Whispr est actuellement inactif.");
        }

        ClientMetadata metadata = clientMetadataResolver.resolve(servletRequest);

        String content = request.content();


        String sentiment = aiSentimentService != null && content != null ? aiSentimentService.analyzeSentiment(content) : "NEUTRAL";

        if ((request.audioUrl() != null && !request.audioUrl().isBlank())) {
            if (content == null || content.isBlank()) {
                String filterLabel = (request.voiceFilter() != null) ? request.voiceFilter() : "NORMAL";
                content = "🎙️ Mémo vocal (" + filterLabel + ")";
            }
        }

        Message message = Message.builder()
                .link(link)
                .content(content)
                .type("SUPER_WHISPR")
                .status("PENDING_PAYMENT")
                .aiCategory(sentiment)
                .audioUrl(request.audioUrl())
                .audioDurationSeconds(request.audioDurationSeconds())
                .voiceFilter(request.voiceFilter())
                .build();
        message = messageRepository.save(message);

        AuditLog auditLog = AuditLog.builder()
                .message(message)
                .hashedIp(metadata.hashedIp())
                .rawIp(metadata.rawIp())
                .userAgent(metadata.userAgent())
                .country(metadata.country())
                .city(metadata.city())
                .referrer(metadata.referrer())
                .build();
        auditLogRepository.save(auditLog);
        message.setAuditLog(auditLog);

        User creator = link.getUser();

        if (stripeSecretKey != null && !stripeSecretKey.isBlank() && !stripeSecretKey.startsWith("mock")) {
            Stripe.apiKey = stripeSecretKey;
            try {
                SessionCreateParams params = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(frontendUrl + "/@" + link.getSlug() + "?payment=success&super_whispr=true")
                        .setCancelUrl(frontendUrl + "/@" + link.getSlug() + "?payment=cancelled")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("eur")
                                                        .setUnitAmount(99L)
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName("⭐ Super Whispr pour @" + creator.getPseudo())
                                                                        .setDescription("Message anonyme prioritaire épinglé en tête d'Inbox")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .putMetadata("type", "SUPER_WHISPR")
                        .putMetadata("messageId", message.getId().toString())
                        .putMetadata("creatorId", creator.getId().toString())
                        .putMetadata("creatorEmail", creator.getEmail())
                        .build();

                Session session = Session.create(params);
                log.info("Session Stripe créée pour Super Whispr: session={} messageId={}", session.getId(), message.getId());
                return new SuperWhisprCheckoutResponse(session.getId(), session.getUrl(), message.getId());
            } catch (StripeException e) {
                log.error("Erreur création session Stripe Super Whispr: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur Stripe: " + e.getMessage());
            }
        }

        String mockSessionId = "mock_sw_" + UUID.randomUUID();
        String mockCheckoutUrl = frontendUrl + "/@" + link.getSlug() + "?payment=success&super_whispr=true&mockSuperWhisprId=" + message.getId();
        log.info("Stripe non configuré, retour session Super Whispr simulée: {}", mockSessionId);
        return new SuperWhisprCheckoutResponse(mockSessionId, mockCheckoutUrl, message.getId());
    }

    @Transactional
    public void processSuperWhisprCompleted(Session session, Event event) {
        Map<String, String> metadata = session.getMetadata();
        String messageIdStr = metadata != null ? metadata.get("messageId") : null;
        String creatorIdStr = metadata != null ? metadata.get("creatorId") : null;

        if (messageIdStr == null || messageIdStr.isBlank()) {
            log.error("messageId manquant pour la session Super Whispr Stripe {}", session.getId());
            return;
        }

        UUID messageId = UUID.fromString(messageIdStr);
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            log.error("Message introuvable pour Super Whispr messageId={}", messageId);
            return;
        }

        if ("UNREAD".equals(message.getStatus())) {
            log.info("Super Whispr déjà validé pour messageId={}", messageId);
            return;
        }

        message.setStatus("UNREAD");
        message.setType("SUPER_WHISPR");
        messageRepository.save(message);

        User creator = null;
        if (creatorIdStr != null && !creatorIdStr.isBlank()) {
            creator = userRepository.findById(UUID.fromString(creatorIdStr)).orElse(null);
        }
        if (creator == null) {
            creator = message.getLink().getUser();
        }

        BigDecimal creatorShare = new BigDecimal("0.50");
        Wallet creatorWallet = getOrCreateWallet(creator);
        creatorWallet.setEarningsEur(creatorWallet.getEarningsEur().add(creatorShare));
        walletRepository.save(creatorWallet);

        Transaction swTx = Transaction.builder()
                .user(creator)
                .type("SUPER_WHISPR")
                .amountEur(creatorShare)
                .coinsDelta(0)
                .description("Super Whispr reçu (+ 0,50 € pour vous)")
                .stripeSessionId(session.getId())
                .stripeEventId(event != null ? event.getId() : "mock_sw_evt_" + UUID.randomUUID())
                .status("COMPLETED")
                .build();
        transactionRepository.save(swTx);

        log.info("Super Whispr validé ! 0.50 EUR crédités au créateur {}", creator.getPseudo());

        if (messagingTemplate != null && messageService != null) {
            try {
                messagingTemplate.convertAndSend("/topic/user/" + creator.getEmail() + "/messages", messageService.toDto(message));
            } catch (Exception e) {
                log.warn("Impossible d'envoyer la notification WebSocket pour le Super Whispr: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public void simulateSuperWhisprPayment(UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message introuvable"));

        if ("UNREAD".equals(message.getStatus())) return;

        message.setStatus("UNREAD");
        message.setType("SUPER_WHISPR");
        messageRepository.save(message);

        User creator = message.getLink().getUser();
        BigDecimal creatorShare = new BigDecimal("0.50");
        Wallet creatorWallet = getOrCreateWallet(creator);
        creatorWallet.setEarningsEur(creatorWallet.getEarningsEur().add(creatorShare));
        walletRepository.save(creatorWallet);

        Transaction swTx = Transaction.builder()
                .user(creator)
                .type("SUPER_WHISPR")
                .amountEur(creatorShare)
                .coinsDelta(0)
                .description("Super Whispr simulé (+ 0,50 €)")
                .stripeSessionId("sim_sw_" + UUID.randomUUID())
                .status("COMPLETED")
                .build();
        transactionRepository.save(swTx);

        if (messagingTemplate != null && messageService != null) {
            try {
                messagingTemplate.convertAndSend("/topic/user/" + creator.getEmail() + "/messages", messageService.toDto(message));
            } catch (Exception ignored) {}
        }
    }


    @Transactional
    public CheckoutSessionResponse createExpressClueCheckoutSession(String userEmail, UUID messageId) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message introuvable"));

        if (!message.getLink().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        long amountCents = 149L;

        if (stripeSecretKey != null && !stripeSecretKey.isBlank() && !stripeSecretKey.startsWith("mock")) {
            Stripe.apiKey = stripeSecretKey;
            try {
                SessionCreateParams params = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(frontendUrl + "/inbox?payment=success&clue_unlock=true&messageId=" + message.getId())
                        .setCancelUrl(frontendUrl + "/inbox?payment=cancelled")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("eur")
                                                        .setUnitAmount(amountCents)
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName("⚡ Démasquage Express des Indices (1 message)")
                                                                        .setDescription("Révélation immédiate des 3 indices secrets (Ville, Appareil, Réseau)")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .putMetadata("type", "EXPRESS_CLUE_UNLOCK")
                        .putMetadata("userId", user.getId().toString())
                        .putMetadata("userEmail", user.getEmail())
                        .putMetadata("messageId", message.getId().toString())
                        .build();

                Session session = Session.create(params);
                log.info("Session Stripe créée pour Démasquage Express: session={} user={} messageId={}", session.getId(), user.getEmail(), message.getId());
                return new CheckoutSessionResponse(session.getId(), session.getUrl());
            } catch (StripeException e) {
                log.error("Erreur création session Stripe Express Clue: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur Stripe: " + e.getMessage());
            }
        }

        String mockSessionId = "mock_clue_" + UUID.randomUUID();
        String mockCheckoutUrl = frontendUrl + "/inbox?payment=success&clue_unlock=true&messageId=" + message.getId() + "&mockClueMessageId=" + message.getId();
        log.info("Stripe non configuré, retour session Démasquage Express simulée: {}", mockSessionId);
        return new CheckoutSessionResponse(mockSessionId, mockCheckoutUrl);
    }

    @Transactional
    public void processExpressClueUnlockCompleted(Session session, Event event) {
        Map<String, String> metadata = session.getMetadata();
        String messageIdStr = metadata != null ? metadata.get("messageId") : null;
        String userIdStr = metadata != null ? metadata.get("userId") : null;

        if (messageIdStr == null || userIdStr == null) {
            log.error("Données manquantes pour session Express Clue: session={}", session.getId());
            return;
        }

        UUID messageId = UUID.fromString(messageIdStr);
        UUID userId = UUID.fromString(userIdStr);

        User user = userRepository.findById(userId).orElse(null);
        Message message = messageRepository.findById(messageId).orElse(null);

        if (user == null || message == null) {
            log.error("User ou Message introuvable pour Express Clue messageId={}", messageId);
            return;
        }

        unlockAllCluesInternal(user, message);

        BigDecimal priceEur = new BigDecimal("1.49");
        Wallet wallet = getOrCreateWallet(user);
        wallet.setTotalSpentEur(wallet.getTotalSpentEur().add(priceEur));
        walletRepository.save(wallet);

        Transaction clueTx = Transaction.builder()
                .user(user)
                .type("EXPRESS_CLUE_PURCHASE")
                .amountEur(priceEur)
                .coinsDelta(0)
                .description("⚡ Démasquage Express 1-clic de tous les indices")
                .stripeSessionId(session.getId())
                .stripeEventId(event != null ? event.getId() : "mock_clue_evt_" + UUID.randomUUID())
                .status("COMPLETED")
                .build();
        transactionRepository.save(clueTx);

        log.info("Démasquage Express validé via Stripe pour user={} messageId={}", user.getEmail(), messageId);

        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/user/" + user.getEmail() + "/clues-unlocked", (Object) message.getId().toString());
            } catch (Exception e) {
                log.warn("Erreur WebSocket Express Clue: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public void simulateExpressClueUnlock(String userEmail, UUID messageId) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message introuvable"));

        unlockAllCluesInternal(user, message);

        BigDecimal priceEur = new BigDecimal("1.49");
        Wallet wallet = getOrCreateWallet(user);
        wallet.setTotalSpentEur(wallet.getTotalSpentEur().add(priceEur));
        walletRepository.save(wallet);

        Transaction clueTx = Transaction.builder()
                .user(user)
                .type("EXPRESS_CLUE_PURCHASE")
                .amountEur(priceEur)
                .coinsDelta(0)
                .description("⚡ Démasquage Express simulé (1,49 €)")
                .stripeSessionId("sim_clue_" + UUID.randomUUID())
                .status("COMPLETED")
                .build();
        transactionRepository.save(clueTx);
    }

    private void unlockAllCluesInternal(User user, Message message) {
        AuditLog audit = message.getAuditLog() != null ? message.getAuditLog() : auditLogRepository.findByMessageId(message.getId()).orElse(null);
        Map<String, String> clues = new LinkedHashMap<>();

        if (audit != null) {
            String city = audit.getCity() != null && !audit.getCity().isBlank() ? audit.getCity() : null;
            String country = audit.getCountry() != null && !audit.getCountry().isBlank() ? audit.getCountry() : "France";
            clues.put("LOCATION", city != null ? city + " (" + country + ")" : country + " (Rayon < 15km)");

            String ua = audit.getUserAgent() != null ? audit.getUserAgent().toLowerCase() : "";
            if (ua.contains("iphone")) clues.put("DEVICE", "Apple iPhone (iOS Safari)");
            else if (ua.contains("android")) clues.put("DEVICE", "Smartphone Android");
            else clues.put("DEVICE", "Navigateur Web");

            int hash = Math.abs(audit.getHashedIp() != null ? audit.getHashedIp().hashCode() : (int) System.currentTimeMillis());
            String[] operators = {"Orange Mobile (4G/5G)", "SFR Fibre / 5G", "Bouygues Telecom", "Free Mobile", "Wi-Fi Haut Débit"};
            clues.put("NETWORK", operators[hash % operators.length]);
        } else {
            clues.put("LOCATION", "France (Rayon < 15km)");
            clues.put("DEVICE", "Smartphone inconnu");
            clues.put("NETWORK", "Opérateur mobile / 4G");
        }

        for (Map.Entry<String, String> entry : clues.entrySet()) {
            if (unlockedClueRepository.findByUserIdAndMessageIdAndClueType(user.getId(), message.getId(), entry.getKey()).isEmpty()) {
                UnlockedClue clue = UnlockedClue.builder()
                        .user(user)
                        .message(message)
                        .clueType(entry.getKey())
                        .clueValue(entry.getValue())
                        .build();
                unlockedClueRepository.save(clue);
            }
        }
    }

}
