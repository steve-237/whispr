package com.whispr.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private int coins = 30;

    @Column(name = "total_spent_eur", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalSpentEur = BigDecimal.ZERO;

    @Column(name = "is_pro", nullable = false)
    @Builder.Default
    private boolean isPro = false;

    @Column(name = "pro_expires_at")
    private ZonedDateTime proExpiresAt;

    @Column(name = "earnings_eur", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal earningsEur = BigDecimal.ZERO;

    @Column(name = "referral_count", nullable = false)
    @Builder.Default
    private int referralCount = 0;

    @Column(name = "affiliate_code", length = 50)
    private String affiliateCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
