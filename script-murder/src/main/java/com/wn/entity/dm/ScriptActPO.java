package com.wn.entity.dm;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @Author: 杜江
 * @Description:存放剧本杀每一幕剧情的数据，专门给 DM 主持人控制游戏分幕流程使用。
 * @DateTime: 2026/6/25 11:01
 * @Component:
 **/
@Data
@Entity
@Table(name = "dm_script_act")
@DynamicInsert
@DynamicUpdate
public class ScriptActPO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "act_id")
    private Long actId;     //单幕剧情唯一编号

    @Column(name = "script_id", nullable = false)
    private Long scriptId;     //剧本 ID

    @Column(name = "act_order", nullable = false)
    private Integer actOrder;       //幕剧情顺序

    @Column(name = "act_name", nullable = false, length = 100)
    private String actName;     //幕剧情名称

    @Column(name = "act_description", columnDefinition = "TEXT")
    private String actDescription;     //幕剧情描述

    @Column(name = "opening_narration", columnDefinition = "TEXT")
    private String openingNarration;     //幕剧情开场白

    @Column(name = "duration_minutes")
    private Integer durationMinutes;     //幕剧情推荐时长（分钟）

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
