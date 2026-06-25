package com.wn.entity.dm;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
/**
 * @Author: 杜江
 * @Description: dm复盘实体
 * @DateTime: 2026/6/25 11:10
 * @Component:
 **/
@Data
@Entity
@Table(name = "dm_script_review")
@DynamicInsert
@DynamicUpdate
public class ScriptReviewPO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;     //复盘id本条复盘记录唯一标识

    @Column(name = "script_id", nullable = false)
    private Long scriptId;     //剧本id，用来绑定当前复盘属于哪一个剧本，和剧本主表做关联

    @Column(name = "murderer_role_id", nullable = false)
    private Long murdererRoleId;     //凶手角色id，绑定凶手对应的角色数据，DM 复盘时可以直接锁定真凶

    @Column(name = "full_review", columnDefinition = "TEXT")
    private String fullReview;          //完整复盘，投票结束后 DM 主持人一次性念出的完整故事真相。

    @Column(name = "correct_ending", columnDefinition = "TEXT")
    private String correctEnding;     //正确结局，玩家投票选出真凶时触发的结局文案

    @Column(name = "wrong_ending", columnDefinition = "TEXT")
    private String wrongEnding;     //错误结局，玩家投错人时触发的坏结局文案

    @Column(name = "trick_explanation", columnDefinition = "TEXT")
    private String trickExplanation;     //手法解释，核心诡计解析，专门用来解释密室手法、作案诡计

    @Column(name = "timeline", columnDefinition = "TEXT")
    private String timeline;     //时间线，梳理所有人行动顺序

    @Column(name = "motivation", columnDefinition = "TEXT")
    private String motivation;     //作案动机，人物恩怨与行凶缘由

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
