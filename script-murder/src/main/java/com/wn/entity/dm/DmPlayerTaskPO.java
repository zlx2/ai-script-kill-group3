package com.wn.entity.dm;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @Author: 杜江
 * @Description:记录单局房间内每个玩家的个人任务进度与角色状态，
 *              给 DM 主持人提供玩家个人任务、秘密暴露情况等数据
 * @DateTime: 2026/6/25 11:27
 * @Component:
 **/
@Data
@Entity
@Table(name = "dm_player_task")
@DynamicInsert
@DynamicUpdate
public class DmPlayerTaskPO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;        ///本条玩家任务记录唯一 ID

    ///房间 ID UUID，用来隔离不同对局，不能为空
    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;

    ///玩家 ID，定位到本局里的某一位玩家
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    ///玩家任务进度，JSON 字符串格式，记录玩家当前任务进度
    @Column(name = "task_progress", columnDefinition = "TEXT")
    private String taskProgress;

    ///玩家是否被禁言，0 表示未禁言，1 表示已禁言 ，默认值为 0 表示未禁言
    @Column(name = "is_muted")
    private Byte isMuted = 0;

    ///玩家是否暴露秘密，0 表示未暴露，1 表示已暴露 ，默认值为 0 表示未暴露
    /// DM 读到这个标记，在剧情里触发对应人物剧情。
    @Column(name = "secret_revealed")
    private Byte secretRevealed = 0;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
