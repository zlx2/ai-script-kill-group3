/**
 * @Author: 弗
 * @Description:角色基础信息添加参数
 * @DateTime: 2026/6/25 15:06
 * @Component:
 **/
package com.wn.entity.script.stage.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScriptRoleAddReq {
    @NotNull(message = "剧本ID不能为空")
    private Long scriptId;

    private Long roleId;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @NotBlank(message = "性别不能为空")
    private String gender;

    @NotNull(message = "年龄不能为空")
    private Integer age;

    @NotBlank(message = "角色背景故事不能为空")
    private String characterStory;

    @NotBlank(message = "角色私人秘密不能为空")
    private String secretInfo;
}
