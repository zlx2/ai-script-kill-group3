/**
 * @Author: 鱼
 * @Description:游戏阶段枚举
 * @DateTime: 2026/6/23 11:30
 * @Component:
 **/
package com.wn.controller.enums.room;

import lombok.Getter;

@Getter
public enum GameStageEnum {

    WAITING("waiting", "等待开始"),
    SELECTING("selecting", "选择角色"),
    READING("reading", "阅读剧本"),
    DISCUSSION("discussion", "公聊讨论"),
    SEARCHING("searching", "搜证阶段"),
    VOTING("voting", "投票阶段"),
    RESULT("result", "复盘阶段");

    private final String code;
    private final String desc;

    GameStageEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static GameStageEnum getByCode(String code) {
        for (GameStageEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}