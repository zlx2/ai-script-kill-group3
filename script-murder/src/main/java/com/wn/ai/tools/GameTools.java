package com.wn.ai.tools;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:29
 * @Component:
 **/

import io.agentscope.core.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 游戏状态查询工具
 * 供NPC Agent和游戏主持Agent调用，查询当前游戏状态
 */
@Component
public class GameTools {

    /**
     * 获取当前游戏阶段
     * @param roomId 房间ID
     * @return 当前阶段
     */
    @Tool(name = "getCurrentStage", description = "获取指定房间的当前游戏阶段")
    public String getCurrentStage(Long roomId) {
        // 实际项目中从Redis或数据库查询
        // 这里简化演示
        return "当前阶段：第一幕-公聊阶段";
    }

    /**
     * 获取玩家已发现的线索
     * @param roomId 房间ID
     * @param userId 玩家ID
     * @return 线索列表
     */
    @Tool(name = "getPlayerClues", description = "获取指定玩家在房间中已发现的线索")
    public String getPlayerClues(Long roomId, Long userId) {
        // 实际项目中从数据库查询
        return "已发现线索：\n- 带血的刀\n- 撕碎的照片\n- 神秘的钥匙";
    }
}
