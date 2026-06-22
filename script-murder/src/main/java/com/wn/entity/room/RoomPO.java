/**
 * @Author: 鱼
 * @Description:游戏房间PO持久化实体，映射game_room表
 * @DateTime: 2026/6/22 15:57
 * @Component:房间模块数据库实体
 **/
package com.wn.entity.room;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "game_room",
        indexes = {
                @Index(name = "idx_host_id", columnList = "host_id"),
                @Index(name = "idx_status", columnList = "room_status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_no", columnNames = "room_no")
        })
@DynamicInsert
@DynamicUpdate
public class RoomPO {
    /**
     * 房间ID UUID主键：UUID 的 roomId 是 36 位字符串，又长又难记，不可能让用户手动输入
     */
    @Id
    @Column(name = "room_id", nullable = false, length = 36)
//    @GeneratedValue(strategy = GenerationType.UUID)
    private String roomId;
    /**
     * 新增前自动生成UUID
     */
    @PrePersist
    public void generateId() {
        // 仅新增时为空才生成UUID，避免覆盖传入ID
        if (this.roomId == null || this.roomId.isBlank()) {
            this.roomId = UUID.randomUUID().toString();
        }
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    /**
     * 房间名称：给玩家看的展示名，偏人类阅读，比如「深夜凶宅发车局」「新手欢乐本专场」，
     * 支持重复，方便玩家一眼看懂房间主题。
     */
    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;

    /**
     * 房间唯一编号：roomNo 是短数字 / 字母，专门给人使用，是核心交互字段
     */
    @Column(name = "room_no", nullable = false, length = 20)
    private String roomNo;

    /**
     * 关联剧本ID 剧本主键 ID，用来把「游戏房间」和「剧本」做关联绑定。
     */
    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    /**
     * 主持人用户ID
     */
    @Column(name = "host_id", nullable = false)
    private Long hostId;

    /**
     * 房间最大容纳玩家数量
     */
    @Column(name = "max_player", nullable = false)
    private Integer maxPlayer;

    /**
     * 当前在线玩家人数，默认0
     */
    @Column(name = "current_player")
    private Integer currentPlayer = 0;

    /**
     * 房间状态：0等待中 1游戏中 2已结束
     */
    @Column(name = "room_status")
    private Byte roomStatus = 0;

    /**
     * 当前游戏轮次，默认0
     */
    @Column(name = "current_round")
    private Integer currentRound = 0;

    /**
     * 当前游戏阶段：waiting等待开局/reading读本阶段/discussion自由讨论/searching搜证环节/voting投票指认/result结局公示
     */
    @Column(name = "current_stage", length = 20)
    private String currentStage = "waiting";

    /**
     * 房间游戏开始时间
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * 房间游戏结束时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 房间访问密码，私密房间使用
     */
    @Column(name = "password", length = 50)
    private String password;

    /**
     * 记录创建时间
     */
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 记录更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;



    /**
     * 更新时仅刷新更新时间
     */
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}

