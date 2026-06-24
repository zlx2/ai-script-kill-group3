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
import java.util.stream.Collectors;

/**
 * 剧本杀全部工具，完全遵循AgentScope V2官方tool文档注解规范
 */
@Component
public class ScriptKillTools {
    // 线索池：key=线索ID，[线索文本,归属角色，all=全公开]
    private final Map<String, String[]> cluePool = Map.of(
            "clue001", new String[]{"【公开】死者茶杯残留氰化物毒素", "all"},
            "clue002", new String[]{"【私有-小兰】我深夜看到老板独自留在书房", "小兰"},
            "clue003", new String[]{"【私有-老李】死者欠我五十万巨款拒不归还", "老李"},
            "clue004", new String[]{"【公开】书房窗户从内部反锁，密室杀人", "all"},
            "clue005", new String[]{"【私有-老王】我给死者开的安眠药无毒", "老王"}
    );

    // 房间->角色->已获取线索ID集合
    private final Map<String, Map<String, Set<String>>> roomPlayerClues = new ConcurrentHashMap<>();
    // 房间投票记录
    private final Map<String, Map<String, Integer>> roomVoteMap = new ConcurrentHashMap<>();
    // 房间-玩家已搜证标记（每人仅一次搜证）
    private final Map<String, Set<String>> roomSearchedRecord = new ConcurrentHashMap<>();

    /**
     * 搜证工具：唯一获取线索渠道
     */
    @Tool(
            name = "search_clue",
            description = "玩家消耗行动点搜证，roomId房间，playerName角色，scene场景，每人仅一次搜证",
            readOnly = true,
            concurrencySafe = true
    )
    public String searchClue(
            @ToolParam(name = "roomId", description = "房间ID") String roomId,
            @ToolParam(name = "playerName", description = "当前搜证角色名") String playerName,
            @ToolParam(name = "scene", description = "场景：tea_room / study_room / maid_room / business_room / doctor_room") String scene
    ) {
        Set<String> searchedSet = roomSearchedRecord.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet());
        if (searchedSet.contains(playerName)) {
            return "【搜证失败】" + playerName + " 已消耗搜证次数，无法重复搜证";
        }

        String clueId = switch (scene) {
            case "tea_room" -> "clue001";
            case "study_room" -> "clue004";
            case "maid_room" -> "clue002";
            case "business_room" -> "clue003";
            case "doctor_room" -> "clue005";
            default -> null;
        };
        if (clueId == null) {
            return "【搜证失败】场景不存在，可选：tea_room / study_room / maid_room / business_room / doctor_room";
        }

        String[] clueInfo = cluePool.get(clueId);
        String clueText = clueInfo[0];

        // 存入当前角色私有线索
        Map<String, Set<String>> playerClueMap = roomPlayerClues.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        Set<String> ownClues = playerClueMap.computeIfAbsent(playerName, k -> ConcurrentHashMap.newKeySet());
        ownClues.add(clueId);
        searchedSet.add(playerName);

        return playerName + " 搜证获得线索：" + clueText;
    }

    /**
     * 查询当前角色自己的全部线索（前端私有面板展示）
     */
    public List<String> getPlayerPrivateClues(String roomId, String roleName) {
        Map<String, Set<String>> playerClueMap = roomPlayerClues.get(roomId);
        if (playerClueMap == null || !playerClueMap.containsKey(roleName)) {
            return List.of();
        }
        return playerClueMap.get(roleName).stream()
                .map(id -> cluePool.get(id)[0])
                .collect(Collectors.toList());
    }

    /**
     * 投票工具
     */
    @Tool(
            name = "vote_suspect",
            description = "投票阶段指认嫌疑人，按房间统计票数",
            readOnly = false,
            concurrencySafe = true
    )
    public String voteSuspect(
            @ToolParam(name = "roomId") String roomId,
            @ToolParam(name = "playerName") String playerName,
            @ToolParam(name = "suspectName") String suspectName
    ) {
        Map<String, Integer> voteMap = roomVoteMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        voteMap.put(suspectName, voteMap.getOrDefault(suspectName, 0) + 1);
        return String.format("%s 投给【%s】，当前房间票数：%s", playerName, suspectName, voteMap);
    }

    // 获取房间最高票角色（DM复盘）
    public String getRoomMaxVoteSuspect(String roomId) {
        Map<String, Integer> voteMap = roomVoteMap.get(roomId);
        if (voteMap == null || voteMap.isEmpty()) return "暂无投票";
        return voteMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("暂无投票");
    }

    // 清空房间全部缓存
    public void clearRoomAllData(String roomId) {
        roomVoteMap.remove(roomId);
        roomSearchedRecord.remove(roomId);
        roomPlayerClues.remove(roomId);
    }

    // 判断玩家是否已搜证
    public boolean playerHasSearched(String roomId, String playerName) {
        Set<String> set = roomSearchedRecord.get(roomId);
        return set != null && set.contains(playerName);
    }

    // 获取全局可选角色列表
    public Set<String> getAllValidRoles() {
        return Set.of("小兰", "老李", "老王");
    }
}
