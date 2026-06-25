package com.wn.entity.dm;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @Author: 杜江
 * @Description:对局房间状态表
 * @DateTime: 2026/6/25 11:22
 * @Component:
 **/
@Data
@Entity
@Table(name = "dm_room_state")
@DynamicInsert
@DynamicUpdate
public class DmRoomStatePO {
    @Id
    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;  ///房间 ID（UUID 格式，长度 36），用来区分不同对局

    ///AI 主持人依靠此字段，读取对应幕的旁白与剧情，自动推进分幕流程，当前剧情幕数，默认值 = 1
    @Column(name = "current_act")
    private Integer currentAct = 1;

    ///是否开启私聊，默认值 = 1 表示开启 0 表示关闭，玩家只能在公开频道聊天
    @Column(name = "is_private_chat_enabled")
    private Byte isPrivateChatEnabled = 1;

    /// 是否允许 NPC AI 对话，1 = 允许玩家和角色聊天，0 = 禁用
    @Column(name = "is_ai_talk_enabled")
    private Byte isAiTalkEnabled = 1;

    /// 搜证总轮次，默认 2 轮。用来限制玩家总共可以搜索线索的次数，防止无限搜证
    @Column(name = "search_round_count")
    private Integer searchRoundCount = 2;

    /// 聊天时长，默认 10 分钟。玩家在聊天频道的聊天时间，超过这个时间后，会自动退出聊天频道
    @Column(name = "chat_duration_minutes")
    private Integer chatDurationMinutes = 10;

    /// 是否开启投票，默认值 = 0 表示关闭 1 表示开启
    @Column(name = "is_voting")
    private Byte isVoting = 0;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
