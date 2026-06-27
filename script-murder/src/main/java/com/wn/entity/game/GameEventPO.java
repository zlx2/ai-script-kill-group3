package com.wn.entity.game;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 游戏事件 — 每次状态变化都落库
 */
@Data
@Entity
@Table(name = "game_event", indexes = {
        @Index(name = "idx_ge_room_id", columnList = "room_id"),
        @Index(name = "idx_ge_event_type", columnList = "event_type"),
        @Index(name = "idx_ge_created_at", columnList = "created_at")
})
public class GameEventPO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "visibility", nullable = false, length = 32)
    private String visibility = "PUBLIC";

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "payload_json", columnDefinition = "JSON")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
