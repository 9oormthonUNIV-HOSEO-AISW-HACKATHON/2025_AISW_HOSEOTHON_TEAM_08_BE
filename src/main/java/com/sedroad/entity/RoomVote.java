package com.sedroad.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_votes",
    uniqueConstraints = @UniqueConstraint(name = "unique_vote", columnNames = {"room_id", "user_id", "recommendation_id"}),
    indexes = {
        @Index(name = "idx_room_id", columnList = "room_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_recommendation_id", columnList = "recommendation_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomVote {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "recommendation_id", nullable = false)
    private TripRecommendation recommendation;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

