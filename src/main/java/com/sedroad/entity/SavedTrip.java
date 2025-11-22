package com.sedroad.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_trips",
    uniqueConstraints = @UniqueConstraint(name = "unique_saved", columnNames = {"user_id", "recommendation_id"}),
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_recommendation_id", columnList = "recommendation_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedTrip {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "recommendation_id", nullable = false)
    private TripRecommendation recommendation;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

