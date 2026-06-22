/**
 * @Author: 鱼
 * @Description:创建房间后返回的值
 * @DateTime: 2026/6/22 16:55
 * @Component:
 **/
package com.wn.controller.room.vo;

import lombok.Data;

@Data
public class CreateRoomResultVO {

    /**
     * 房间ID，防止重复
     */
    private Long roomId;

    /**
     * 房间号，用户需要
     */
    private String roomNo;

    /**
     * 房间名称
     */
    private String roomName;
}