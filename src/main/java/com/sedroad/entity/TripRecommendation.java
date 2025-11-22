package com.sedroad.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip_recommendations", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_room_id", columnList = "room_id"),
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripRecommendation {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSON")
    private List<String> course;

    @Column(columnDefinition = "TEXT")
    private String why;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private Map<String, String> options;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private Map<String, Integer> satisfaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('personal', 'generation', 'room')")
    private Type type;

    @Column(name = "estimated_time", length = 50)
    private String estimatedTime;

    @Column(name = "estimated_cost", length = 50)
    private String estimatedCost;

    @Column(name = "talking_tip", columnDefinition = "TEXT")
    private String talkingTip;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "for_generation", length = 20)
    private String forGeneration;

    @Column(name = "room_name", length = 255)
    private String roomName;

    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_adjustment", columnDefinition = "JSON")
    private Map<String, Object> aiAdjustment;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Type {
        personal, generation, room
    }
}

