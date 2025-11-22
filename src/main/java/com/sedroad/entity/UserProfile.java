package com.sedroad.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 50")
    private Integer speed = 50;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 50")
    private Integer stamina = 50;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 50")
    private Integer budget = 50;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 50")
    private Integer photo = 50;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 50")
    private Integer tradition = 50;

    @Column(length = 50)
    private String tag;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

