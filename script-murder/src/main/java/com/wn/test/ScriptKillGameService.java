/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/23 12:05
 * @Component:
 **/
package com.wn.test;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScriptKillGameService {
    // 角色基础配置：角色名 = [对外身份, 隐藏秘密]
    private final Map<String, String[]> ROLE_MAP = Map.of(
            "小兰", new String[]{"庄园女仆", "我深夜下毒杀死老爷，伪造密室"},
            "老李", new String[]{"合作商人", "死者欠我巨额欠款，我有杀人动机"},
            "老王", new String[]{"私人医生", "我仅提供普通安眠药，不含毒素"}
    );

    // 游戏阶段
    public enum GameStage {
        OPEN, SELF_INTRO, SEARCH, DISCUSS, VOTE, REVIEW
    }

    // ========== 房间缓存隔离 ==========
    // 1. 全局公聊历史：所有玩家都能看到（DM发言、公聊、公开搜证、投票）
    private final Map<String, StringBuilder> roomGlobalChat = new ConcurrentHashMap<>();
    // 2. 角色私有对话缓存：roomId -> 角色名 -> 私有记录（私有线索、私聊、专属DM消息）
    private final Map<String, Map<String, StringBuilder>> roomPrivateChat = new ConcurrentHashMap<>();
    // 3. 房间已被选择角色（防止重复选）
    private final Map<String, Set<String>> roomSelectedRoles = new ConcurrentHashMap<>();
    // 4. 当前游戏阶段
    private final Map<String, GameStage> roomStage = new ConcurrentHashMap<>();
    // 5. 已完成自我介绍角色集合
    private final Map<String, Set<String>> roomIntroDone = new ConcurrentHashMap<>();

    @Resource
    private ReActAgent dmAgent;
    @Resource
    private ScriptKillTools scriptKillTools;

    // DM基础调用封装
    private String dmCall(String roomId, String prompt) {
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(prompt)
                .build();
        RuntimeContext ctx = RuntimeContext.builder().sessionId("room_" + roomId).build();
        String reply = dmAgent.call(List.of(msg), ctx).block().getTextContent();
        // DM全局消息存入公聊
        recordGlobalChat(roomId, "【DM】" + reply);
        return reply;
    }

    // 1. 创建空房间，进入选角色阶段
    public String startGame(String roomId) {
        roomGlobalChat.put(roomId, new StringBuilder());
        roomPrivateChat.put(roomId, new ConcurrentHashMap<>());
        roomSelectedRoles.put(roomId, ConcurrentHashMap.newKeySet());
        roomStage.put(roomId, GameStage.OPEN);
        roomIntroDone.put(roomId, ConcurrentHashMap.newKeySet());
        scriptKillTools.clearRoomAllData(roomId);

        String prompt = """
                你是剧本杀DM，介绍案情：庄园主张老爷死于书房密室，茶杯检出氰化物毒素。
                只进行简单的剧情背景介绍，不要透露凶手。
                当前阶段玩家自主选择角色，可选：小兰、老李、老王，每个角色只能一人选择。
                全部人选完角色后进入自我介绍阶段。
                """;
        String dmReply = dmCall(roomId, prompt);
        roomStage.put(roomId, GameStage.OPEN);
        return "房间创建成功，当前阶段：角色选择阶段\nDM：" + dmReply;
    }

    // 玩家自选角色
    public String selectRole(String roomId, String roleName) {
        Set<String> validRoles = scriptKillTools.getAllValidRoles();
        if (!validRoles.contains(roleName)) return "角色不存在，可选：" + validRoles;
        Set<String> selected = roomSelectedRoles.get(roomId);
        if (selected.contains(roleName)) return "该角色已被占用，请更换";

        selected.add(roleName);
        // 初始化该角色私有记录容器
        roomPrivateChat.get(roomId).putIfAbsent(roleName, new StringBuilder());
        String identity = ROLE_MAP.get(roleName)[0];
        String log = String.format("【选角成功】%s 选择角色，身份：%s", roleName, identity);
        recordGlobalChat(roomId, log);
        return log;
    }

    // ========== DM操作：分发私有线索（存入目标角色私有记录） ==========
    public String dmDistributePrivateClue(String roomId, String targetRole, String clueId) {
        Set<String> roomRoles = roomSelectedRoles.get(roomId);
        if (!roomRoles.contains(targetRole)) return "目标角色未选择，无法分发线索";

        String result = scriptKillTools.distributeClue(roomId, targetRole, clueId);
        // 私有线索仅写入该角色私有历史，不进全局公聊
        recordPrivateChat(roomId, targetRole, "【DM专属线索】" + result);
        // 全局仅记录分发行为摘要，不泄露线索内容
        recordGlobalChat(roomId, "【DM操作】向" + targetRole + "分发专属线索");
        return result;
    }

    // ========== 搜证（公开线索进全局，私有线索仅存入角色私有记录） ==========
    public String searchEvidence(String roomId, String roleName, String scene) {
        GameStage stage = roomStage.get(roomId);
        if (!GameStage.SEARCH.equals(stage)) return "当前非搜证阶段";
        Set<String> roomRoles = roomSelectedRoles.get(roomId);
        if (!roomRoles.contains(roleName)) return "请先选择角色再搜证";

        String clueResult = scriptKillTools.searchClue(roomId, roleName, scene);
        String clueText = clueResult.split("搜到线索：")[1];
        // 区分公开/私有，分别存入对应记录
        if (clueText.contains("【公开】")) {
            recordGlobalChat(roomId, "【搜证-" + roleName + "】" + clueResult);
        } else {
            recordGlobalChat(roomId, "【搜证-" + roleName + "】获得一条私有线索（仅本人可见）");
            recordPrivateChat(roomId, roleName, "【搜证私有线索】" + clueResult);
        }
        return clueResult;
    }

    // ========== 投票接口（全局可见） ==========
    public String voteSuspect(String roomId, String voter, String target) {
        GameStage stage = roomStage.get(roomId);
        if (!GameStage.VOTE.equals(stage)) return "当前非投票阶段";
        Set<String> roomRoles = roomSelectedRoles.get(roomId);
        if (!roomRoles.contains(voter)) return "请先选择角色再投票";

        String voteLog = scriptKillTools.voteSuspect(roomId, voter, target);
        recordGlobalChat(roomId, "【投票】" + voteLog);
        return voteLog;
    }

    // ========== 自我介绍（全局可见） ==========
    public String selfIntro(String roomId, String roleName) {
        Set<String> roomRoles = roomSelectedRoles.get(roomId);
        if (!roomRoles.contains(roleName)) return "未选择角色，无法自我介绍";
        GameStage stage = roomStage.get(roomId);
        if (!GameStage.SELF_INTRO.equals(stage)) return "当前非自我介绍阶段";

        String identity = ROLE_MAP.get(roleName)[0];
        String prompt = String.format("玩家【%s】完成自我介绍，简单友好回应，不泄露凶手和他人秘密，身份：%s", roleName, identity);
        String dmReply = dmCall(roomId, prompt);
        roomIntroDone.get(roomId).add(roleName);

        String log = String.format("【%s自我介绍】DM回应：%s", roleName, dmReply);
        recordGlobalChat(roomId, log);

        // 全部角色自我介绍完成自动切搜证阶段
        Set<String> done = roomIntroDone.get(roomId);
        Set<String> allRoomRoles = roomSelectedRoles.get(roomId);
        if (done.size() == allRoomRoles.size()) {
            dmCall(roomId, """
                    所有玩家自我介绍完毕，进入搜证阶段，每人仅一次搜证机会，可选场景：
                    tea_room / study_room / maid_room / business_room / doctor_room
                    搜证获得的私有线索仅自己可见，公开线索全房间可见。
                    """);
            roomStage.put(roomId, GameStage.SEARCH);
        }
        return log;
    }

    // ========== 1. 全局公聊发言（所有角色可见） ==========
    public String publicChat(String roomId, String roleName, String content) {
        GameStage stage = roomStage.get(roomId);
        if (GameStage.SELF_INTRO.equals(stage)) return "自我介绍阶段禁止公聊";
        Set<String> roomRoles = roomSelectedRoles.get(roomId);
        if (!roomRoles.contains(roleName)) return "未选择角色，无法发言";

        String log = String.format("【公聊-%s】：%s", roleName, content);
        recordGlobalChat(roomId, log);
        return log;
    }

    // ========== 2. 私聊发言（仅发送方+接收方私有记录可见，全局不展示） ==========
    public String privateChat(String roomId, String sender, String receiver, String content) {
        GameStage stage = roomStage.get(roomId);
        if (GameStage.SELF_INTRO.equals(stage)) return "自我介绍阶段禁止私聊";
        Set<String> roomRoles = roomSelectedRoles.get(roomId);
        if (!roomRoles.contains(sender) || !roomRoles.contains(receiver)) {
            return "发送方或接收方未选择角色";
        }
        if (sender.equals(receiver)) return "不能和自己私聊";

        String log = String.format("【私聊-%s→%s】：%s", sender, receiver, content);
        // 私聊分别存入双方私有缓存，全局公聊无记录，保护隐私
        recordPrivateChat(roomId, sender, log);
        recordPrivateChat(roomId, receiver, log);
        return log;
    }

    // ========== 阶段切换 ==========
    public String nextGameStage(String roomId) {
        GameStage current = roomStage.get(roomId);
        String prompt = switch (current) {
            case OPEN -> """
                    所有玩家角色选择完毕，进入自我介绍阶段，请依次完成自我介绍。
                    """;
            case SEARCH -> """
                    搜证阶段结束，进入公聊推理阶段，玩家可分享公开线索、互相质问。
                    """;
            case DISCUSS -> """
                    公聊结束，进入投票阶段，玩家可投票指认凶手。
                    """;
            case VOTE -> {
                String maxSus = scriptKillTools.getRoomMaxVoteSuspect(roomId);
                yield String.format("投票结束，最高票嫌疑人：%s，完整复盘全部案件真相、密室下毒手法、动机。", maxSus);
            }
            default -> "无法切换阶段，当前阶段：" + current;
        };
        String dmReply = dmCall(roomId, prompt);
        switch (current) {
            case OPEN -> roomStage.put(roomId, GameStage.SELF_INTRO);
            case SEARCH -> roomStage.put(roomId, GameStage.DISCUSS);
            case DISCUSS -> roomStage.put(roomId, GameStage.VOTE);
            case VOTE -> roomStage.put(roomId, GameStage.REVIEW);
            default -> {}
        }
        return "切换新阶段：" + roomStage.get(roomId) + "\nDM：" + dmReply;
    }

    // ========== 历史记录读写工具 ==========
    // 写入全局公聊（所有人可见）
    private void recordGlobalChat(String roomId, String text) {
        roomGlobalChat.get(roomId).append(text).append("\n");
    }
    // 写入角色私有记录（仅当前角色可见）
    private void recordPrivateChat(String roomId, String roleName, String text) {
        Map<String, StringBuilder> privateMap = roomPrivateChat.get(roomId);
        privateMap.computeIfAbsent(roleName, k -> new StringBuilder()).append(text).append("\n");
    }

    // 接口1：获取全局公聊全部记录（所有玩家通用）
    public String getGlobalHistory(String roomId) {
        StringBuilder sb = roomGlobalChat.get(roomId);
        return sb == null ? "房间不存在" : sb.toString();
    }

    // 接口2：获取当前角色私有历史（仅本人可查看，含私有线索、私聊）
    public String getRolePrivateHistory(String roomId, String roleName) {
        Map<String, StringBuilder> privateMap = roomPrivateChat.get(roomId);
        if (!privateMap.containsKey(roleName)) return "暂无你的私有记录";
        return privateMap.get(roleName).toString();
    }

    // 接口3：查询角色自身全部私有线索（前端线索面板）
    public List<String> getRolePrivateClues(String roomId, String roleName) {
        return scriptKillTools.getPlayerPrivateClues(roomId, roleName);
    }

    // 辅助接口：查询房间已选角色列表
    public Set<String> getRoomSelectedRoles(String roomId) {
        return roomSelectedRoles.getOrDefault(roomId, Set.of());
    }

    // 辅助接口：查询当前游戏阶段
    public GameStage getCurrentStage(String roomId) {
        return roomStage.get(roomId);
    }

    // 销毁房间，清空所有缓存防止内存泄漏
    public void destroyRoom(String roomId) {
        roomGlobalChat.remove(roomId);
        roomPrivateChat.remove(roomId);
        roomSelectedRoles.remove(roomId);
        roomStage.remove(roomId);
        roomIntroDone.remove(roomId);
        scriptKillTools.clearRoomAllData(roomId);
    }
}