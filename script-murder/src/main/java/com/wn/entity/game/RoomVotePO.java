package com.wn.entity.game;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 房间投票记录
 */
@Data
@Entity
@Table(name = "room_vote", indexes = {
        @Index(name = "idx_rv_room_id", columnList = "room_id"),
        @Index(name = "idx_rv_user_id", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_vote_user", columnNames = {"room_id", "user_id"})
})
public class RoomVotePO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 投票目标角色ID */
    @Column(name = "target_role_id")
    private Long targetRoleId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
