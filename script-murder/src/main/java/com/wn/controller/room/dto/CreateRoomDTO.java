/**
 * @Author: 鱼
 * @Description:创建房间参数
 * @DateTime: 2026/6/22 16:33
 * @Component:
 **/
package com.wn.controller.room.dto;

import lombok.Data;

@Data
public class CreateRoomDTO {

    /**
     * 剧本ID
     */
    private Long scriptId;

    /**
     * 房间名称
     */
    private String roomName;

    /**
     * 房间密码（可以选择是否使用密码）
     */
    private String password;
}