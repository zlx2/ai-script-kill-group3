/**
 * @Author: 弗
 * @Description:剧本实体类
 * @DateTime: 2026/6/22 14:02
 * @Component:
 **/
package com.wn.entity.script;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "script_info")
@DynamicInsert
@DynamicUpdate
public class ScriptPO {
    /**
     * 剧本ID 自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    /**
     * 剧本名称
     */
    @Column(name = "script_name", nullable = false, length = 100)
    private String scriptName;

    /**
     * 封面图片
     */
    @Column(name = "cover_image", length = 255)
    private String coverImage;

    /**
     * 剧本类型：悬疑/恐怖/情感/欢乐/硬核
     */
    @Column(name = "script_type", nullable = false, length = 50)
    private String scriptType;

    /**
     * 难度：1简单 2中等 3困难
     */
    @Column(name = "difficulty")
    private Byte difficulty = 2;

    /**
     * 玩家人数
     */
    @Column(name = "player_count", nullable = false)
    private Integer playerCount;

    /**
     * 男性角色数
     */
    @Column(name = "male_count")
    private Integer maleCount = 0;

    /**
     * 女性角色数
     */
    @Column(name = "female_count")
    private Integer femaleCount = 0;

    /**
     * 预计时长（小时）
     */
    @Column(name = "duration")
    private Integer duration = 3;

    /**
     * 剧本简介
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 背景故事
     */
    @Column(name = "background_story", columnDefinition = "TEXT")
    private String backgroundStory;

    /**
     * 剧本价格
     */
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price = new BigDecimal("0.00");

    /**
     * 作者
     */
    @Column(name = "author", length = 50)
    private String author;

    /**
     * 剧本来源：平台AI生成/用户：0否 1是
     */
    @Column(name = "is_ai_generated")
    private Byte isAiGenerated = 0;

    /**
     * 状态：0待审核 1已上架 2已下架
     */
    @Column(name = "status")
    private Byte status = 0;

    /**
     * 游玩次数
     */
    @Column(name = "play_count")
    private Integer playCount = 0;

    /**
     * 评分
     */
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating = new BigDecimal("0.00");

    /*
     * 创建时间
     */
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除 0未删 1已删
     */
    @Column(name = "deleted")
    private Byte deleted = 0;

    /**
     * 自动填充创建/更新时间，配合 @PrePersist @PreUpdate
     */
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
