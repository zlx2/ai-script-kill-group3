/**
 * @Author: 鱼
 * @Description:房间状态枚举
 * @DateTime: 2026/6/23 11:30
 * @Component:
 **/
package com.wn.controller.enums.room;


import lombok.Getter;

@Getter
public enum RoomStatusEnum {

    WAITING((byte)0, "等待中"),
    PLAYING((byte)1, "游戏中"),
    ENDED((byte)2, "已结束");

    // 把Integer改成Byte，和RoomPO的roomStatus字段匹配
    private final Byte code;
    private final String desc;

    RoomStatusEnum(Byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 参数同步改为Byte
    public static RoomStatusEnum getByCode(Byte code) {
        for (RoomStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
