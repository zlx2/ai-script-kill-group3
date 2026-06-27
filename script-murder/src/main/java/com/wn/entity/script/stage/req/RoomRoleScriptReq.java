/**
 * @Author: 弗
 * @Description: 获取房间角色分幕剧情的参数
 * @DateTime: 2026/6/25 11:09
 * @Component: 
 **/
package com.wn.entity.script.stage.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomRoleScriptReq {
    @NotBlank(message = "房间roomId不能为空")
    private String roomId;

    @NotNull(message = "用户userId不能为空")
    private Long userId;
}
