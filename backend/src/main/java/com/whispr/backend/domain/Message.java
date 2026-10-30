package com.whispr.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 20)
    private String type;

    @Column(length = 20)
    private String status;

    @Column(name = "ai_category", length = 50)
    private String aiCategory;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "toxicity_score", precision = 5, scale = 4)
    private BigDecimal toxicityScore;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "audio_duration_seconds")
    private Integer audioDurationSeconds;

    @Column(name = "voice_filter", length = 50)
    private String voiceFilter;

    @Column(name = "tip_amount_eur", precision = 10, scale = 2)
    private BigDecimal tipAmountEur;

    @Column(name = "is_tip_paid", nullable = false)
    @Builder.Default
    private boolean isTipPaid = false;

    @Column(name = "stripe_tip_session_id")
    private String stripeTipSessionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private AuditLog auditLog;
}
