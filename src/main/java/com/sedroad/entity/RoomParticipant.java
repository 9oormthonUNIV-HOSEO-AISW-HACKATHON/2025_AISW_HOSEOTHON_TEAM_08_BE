package com.sedroad.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_participants", 
    uniqueConstraints = @UniqueConstraint(name = "unique_participant", columnNames = {"room_id", "user_id"}),
    indexes = {
        @Index(name = "idx_room_id", columnList = "room_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_joined_at", columnList = "joined_at")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomParticipant {
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('owner', 'member') DEFAULT 'member'")
    private Role role = Role.member;

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    public enum Role {
        owner, member
    }
}

