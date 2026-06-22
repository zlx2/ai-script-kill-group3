/**
 * @Author: 鱼
 * @Description:房间的玩家VO
 * @DateTime: 2026/6/22 19:06
 * @Component:
 **/
package com.wn.controller.room.vo;

import lombok.Data;

@Data
public class RoomPlayerVO {
    private Long userId;//玩家ID
    private String nickname;//玩家昵称
    private String avatar;//玩家头像地址
    private Integer isReady;//是否准备：0未准备 1已准备
    private Integer isHost;//是否房主：0非房主 1房主
    private Long roleId;//玩家角色ID,开局为null
    private String roleName;//玩家角色名称,开局为null
}