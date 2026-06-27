/**
 * @Author: 弗
 * @Description:添加角色分幕剧情参数
 * @DateTime: 2026/6/25 14:39
 * @Component:
 **/
package com.wn.entity.script.stage.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleStageContentAddReq {
    @NotNull(message = "剧本ID不能为空")
    private Long scriptId;
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    @NotNull(message = "分幕stageId不能为空")
    private Long stageId;
    @NotBlank(message = "主线剧情content不能为空")
    private String mainContent;
    private String hintContent;
    private String unlockStage;
}
