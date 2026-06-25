/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 14:39
 * @Component:
 **/
package com.wn.entity.script.stage.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomUserRoleAddReq {
    @NotBlank(message = "房间roomId不能为空")
    private String roomId;
    @NotNull(message = "用户userId不能为空")
    private Long userId;
    @NotNull(message = "剧本scriptId不能为空")
    private Long scriptId;
    @NotNull(message = "角色roleId不能为空")
    private Long roleId;
}
