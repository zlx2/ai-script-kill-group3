package com.wn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:53
 * @Component:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("script_info")
public class ScriptInfo {
    @TableId(type = IdType.AUTO)
    private Long scriptId;
    private String scriptName;
    private String scriptType;
    private String description;
    private Integer status;
    private String characterStory;
    private String secretInfo;
    private String theme;
    private Integer playerCount;
    private String outline;
}
