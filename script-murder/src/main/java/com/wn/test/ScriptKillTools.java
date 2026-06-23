/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/23 12:05
 * @Component:
 **/
package com.wn.test;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 剧本杀全部工具，完全遵循AgentScope V2官方tool文档注解规范
 */
@Component
public class ScriptKillTools {
    // 全局线索库
    private final List<String> cluePool = List.of(
            "【公开】死者茶杯残留氰化物毒素",
            "【私有-女仆小兰】我深夜看到老板独自留在书房",
            "【私有-商人老李】死者欠我五十万巨款拒不归还",
            "【公开】书房窗户从内部反锁，密室杀人",
            "【私有-医生老王】我给死者开的安眠药无毒"
    );

    // 多房间隔离投票：roomId -> {角色:票数}
    private final Map<String, Map<String, Integer>> roomVoteMap = new ConcurrentHashMap<>();
    // 房间-玩家已搜证记录，每人单局仅一次搜证
    private final Map<String, Set<String>> roomSearchedRecord = new ConcurrentHashMap<>();

    /**
     * 搜证工具，供AI ReAct自动调用 / 手动接口调用
     */
    @Tool(
            name = "search_clue",
            description = "玩家消耗行动点搜证，传入房间ID、玩家名称、搜证场景，返回对应线索，每人仅可搜证一次",
            readOnly = true,
            concurrencySafe = true
    )
    public String searchClue(
            @ToolParam(name = "roomId", description = "游戏房间唯一ID，用于房间数据隔离") String roomId,
            @ToolParam(name = "playerName", description = "当前搜证玩家名称") String playerName,
            @ToolParam(name = "scene", description = "搜证场景，可选：tea_room / study_room / maid_room / business_room / doctor_room") String scene
    ) {
        // 校验是否已搜证
        Set<String> searchedSet = roomSearchedRecord.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet());
        if (searchedSet.contains(playerName)) {
            return "【搜证失败】" + playerName + " 已使用搜证机会，无法再次搜证";
        }

        String clue = switch (scene) {
            case "tea_room" -> playerName + " 搜到线索：" + cluePool.get(0);
            case "study_room" -> playerName + " 搜到线索：" + cluePool.get(3);
            case "maid_room" -> playerName + " 搜到线索：" + cluePool.get(1);
            case "business_room" -> playerName + " 搜到线索：" + cluePool.get(2);
            case "doctor_room" -> playerName + " 搜到线索：" + cluePool.get(4);
            default -> "【搜证失败】无该场景线索，可选场景：tea_room / study_room / maid_room / business_room / doctor_room";
        };

        // 标记已搜证
        searchedSet.add(playerName);
        return clue;
    }

    /**
     * 投票指认凶手工具
     */
    @Tool(
            name = "vote_suspect",
            description = "投票阶段玩家指认嫌疑人，按房间隔离票数统计",
            readOnly = false,
            concurrencySafe = true
    )
    public String voteSuspect(
            @ToolParam(name = "roomId", description = "游戏房间ID") String roomId,
            @ToolParam(name = "playerName", description = "投票玩家名称") String playerName,
            @ToolParam(name = "suspectName", description = "嫌疑人：小兰 / 老李 / 老王") String suspectName
    ) {
        Map<String, Integer> voteMap = roomVoteMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        voteMap.put(suspectName, voteMap.getOrDefault(suspectName, 0) + 1);
        return String.format("%s 完成投票，投给【%s】，房间当前票数：%s", playerName, suspectName, voteMap);
    }

    /**
     * 获取房间最高票角色（DM复盘使用）
     */
    public String getRoomMaxVoteSuspect(String roomId) {
        Map<String, Integer> voteMap = roomVoteMap.get(roomId);
        if (voteMap == null || voteMap.isEmpty()) {
            return "暂无玩家投票";
        }
        return voteMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("暂无玩家投票");
    }

    // 清空房间全部缓存数据
    public void clearRoomAllData(String roomId) {
        roomVoteMap.remove(roomId);
        roomSearchedRecord.remove(roomId);
    }

    // 获取房间玩家搜证状态
    public boolean playerHasSearched(String roomId, String playerName) {
        Set<String> set = roomSearchedRecord.get(roomId);
        return set != null && set.contains(playerName);
    }
}
