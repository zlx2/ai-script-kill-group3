
/**
 * @Author: 鱼
 * @Description:玩家房间关联PO持久化实体，映射game_room_player表
 * @DateTime: 2026/6/23 20:50
 * @Component:房间模块玩家关联数据库实体
 **/
package com.wn.entity.room;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "game_room_player",
        indexes = {
                @Index(name = "idx_room_id", columnList = "room_id"),
                @Index(name = "idx_user_id", columnList = "user_id")
        })
@DynamicInsert
@DynamicUpdate
public class RoomPlayerPO {
    /**
     * 自增主键ID
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联房间主键roomId（game_room表room_id，UUID字符串）
     */
    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;

    /**
     * 玩家用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 本局分配的角色ID，未分配角色时为null
     */
    @Column(name = "role_id")
    private Long roleId;

    /**
     * 是否准备：0未准备 1已准备
     */
    @Column(name = "is_ready")
    private Byte isReady = 0;

    /**
     * 是否房主：0否 1是
     */
    @Column(name = "is_host")
    private Byte isHost = 0;

    /**
     * 玩家加入房间时间，插入自动填充当前时间
     */
    @Column(name = "join_time", nullable = false, updatable = false)
    private LocalDateTime joinTime;

    /**
     * 离开房间时间，未离开为null
     */
    @Column(name = "leave_time")
    private LocalDateTime leaveTime;

    /**
     * 本局最终得分，默认0
     */
    @Column(name = "final_score")
    private Integer finalScore = 0;

    /**
     * 投票目标角色ID，本轮投票指向的凶手角色，未投票为null
     */
    @Column(name = "vote_target")
    private Long voteTarget;

    /**
     * 投票是否正确：0投错 1投对，未投票为null
     */
    @Column(name = "is_correct")
    private Byte isCorrect;

    /**
     * 新增玩家记录自动填充加入时间
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.joinTime = now;
    }
}