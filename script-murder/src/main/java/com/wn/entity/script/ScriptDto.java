/**
 * @Author: 弗
 * @Description:接收前端新增剧本信息
 * @DateTime: 2026/6/22 16:32
 * @Component:
 **/
package com.wn.entity.script;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScriptDto {
    private Long scriptId;
    /**
     * 剧本名称
     */
    private String scriptName;
    /**
     * 封面图片
     */
    private String coverImage;
    /**
     * 剧本类型：悬疑/恐怖/情感/欢乐/硬核
     */
    private String scriptType;
    /**
     * 难度：1简单 2中等 3困难
     */
    private Byte difficulty = 2;
    /**
     * 玩家人数
     */
    private Integer playerCount;
    /**
     * 男性角色数
     */
    private Integer maleCount = 0;
    /**
     * 女性角色数
     */
    private Integer femaleCount = 0;
    /**
     * 预计时长（分钟）
     */
    private Integer duration = 120;
    /**
     * 剧本简介
     */
    private String description;

    /**
     * 背景故事
     */
    private String backgroundStory;

    /**
     * 剧本价格
     */
    private BigDecimal price = new BigDecimal("0.00");
    /**
     * 作者
     */
    private String author;
    /**
     * 平台AI生成/用户：0否 1是
     */
    private Byte isAiGenerated = 0;
    /**
     * 状态：0待审核 1已上架 2已下架
     */
    private Byte status = 0;
    /**
     * 游玩次数
     */
    private Integer playCount = 0;
    /**
     * 评分
     */
    private BigDecimal rating = new BigDecimal("0.00");
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 逻辑删除 0未删 1已删
     */
    private Byte deleted = 0;
}
