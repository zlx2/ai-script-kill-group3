/**
 * @Author: 鱼
 * @Description:房间详情视图VO，返回前端房间完整信息（包含剧本、房主、在线玩家）
 * @DateTime: 2026/6/22 19:01
 * @Component:房间模块前端展示对象
 **/
package com.wn.controller.room.vo;

import lombok.Data;

import java.util.List;

@Data
public class RoomDetailVO {
    private String roomId;//房间ID
    private String roomNo;//房间短编号
    private String roomName;//房间自定义名称
    private Integer roomStatus;//房间状态
    private String currentStage;//当前游戏阶段:waiting等待开局/reading读本阶段/discussion自由讨论/searching搜证环节/voting投票指认/result结局公示
    private Integer currentRound;//当前游戏轮次

    private Long scriptId;//剧本ID
    private String scriptName;//剧本名称
    private String scriptCover;//剧本封面的图片地址
    private String scriptType;//剧本类型
    private Integer playerCount;//剧本人数

    private Long hostId;//房主ID
    private String hostNickname;//房主昵称
    private String hostAvatar;//房主头像

    private List<RoomPlayerVO> players;//房间玩家列表
    private Integer currentPlayer;//当前在线玩家数
    private Boolean hasPassword;//房间是否需要密码
}
