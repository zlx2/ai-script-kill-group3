/**
 * @Author: 鱼
 * @Description: 加入房间请求
 * @DateTime: 2026/6/22 19:12
 * @Component:
 **/
package com.wn.controller.room.dto;

import lombok.Data;

@Data
public class JoinRoomDTO {
    private String roomNo;      // 房间号
    private String password;    // 房间密码（可选）
}
