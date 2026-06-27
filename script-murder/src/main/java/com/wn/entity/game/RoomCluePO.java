package com.wn.entity.game;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 房间线索状态
 */
@Data
@Entity
@Table(name = "room_clue", indexes = {
        @Index(name = "idx_rc_room_id", columnList = "room_id"),
        @Index(name = "idx_rc_clue_id", columnList = "clue_id")
})
public class RoomCluePO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;

    @Column(name = "clue_id", nullable = false)
    private Long clueId;

    /** 发现线索的玩家 */
    @Column(name = "discovered_by")
    private Long discoveredBy;

    /** PRIVATE / PUBLIC */
    @Column(name = "visibility", nullable = false, length = 20)
    private String visibility = "PRIVATE";

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
