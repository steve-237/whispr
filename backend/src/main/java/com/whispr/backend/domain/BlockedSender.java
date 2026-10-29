package com.whispr.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "blocked_senders", uniqueConstraints = {
        @UniqueConstraint(name = "unique_user_blocked_ip", columnNames = {"user_id", "blocked_hashed_ip"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedSender {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "blocked_hashed_ip", nullable = false, length = 64)
    private String blockedHashedIp;

    @Column(length = 255)
    @Builder.Default
    private String reason = "Expéditeur bloqué par le destinataire";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;
}
