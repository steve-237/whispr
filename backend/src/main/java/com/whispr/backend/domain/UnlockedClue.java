package com.whispr.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "unlocked_clues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnlockedClue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "clue_type", nullable = false, length = 50)
    private String clueType; // 'LOCATION', 'DEVICE', 'NETWORK'

    @Column(name = "clue_value", nullable = false)
    private String clueValue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;
}
