package com.wn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:54
 * @Component:
 **/
@Data
@TableName("script_role")
public class ScriptRole {
    @TableId(type = IdType.AUTO)
    private Long roleId;
    private Long scriptId;
    private String roleName;
    private String gender;
    private Integer age;
    private String characterStory;
    private String secretInfo;
}
