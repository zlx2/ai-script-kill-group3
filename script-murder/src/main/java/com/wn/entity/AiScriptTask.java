package com.wn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:49
 * @Component:
 **/
@Data
@TableName("ai_script_task")
public class AiScriptTask {
    @TableId(type = IdType.AUTO)
    private Long taskId;
    private Long userId;
    private String scriptTheme;
    private String scriptType;
    private Integer playerCount;
    private Integer difficulty;
    private String backgroundDesc;
    private Integer taskStatus;
    private Integer progress;
    private Long scriptId;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
