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
    // 角色配置：角色名 = [对外公开身份, 隐藏秘密(凶手独有)]
    private final Map<String, String[]> ROLE_MAP = Map.of(
            "小兰", new String[]{"庄园女仆", "我深夜下毒杀死老爷，伪造密室"},
            "老李", new String[]{"合作商人", "死者欠我巨额欠款，我有杀人动机"},
            "老王", new String[]{"私人医生", "我仅提供普通安眠药，不含毒素"}
    );

    // 游戏阶段枚举
    public enum GameStage {
        OPEN, SELF_INTRO, SEARCH, DISCUSS, VOTE, REVIEW
    }

    // 房间内存缓存
    private final Map<String, StringBuilder> roomHistory = new ConcurrentHashMap<>();
    public final Map<String, String> roomHumanRole = new ConcurrentHashMap<>();
    private final Map<String, GameStage> roomStage = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomIntroDone = new ConcurrentHashMap<>();

    @Resource
    private ReActAgent dmAgent;
    @Resource
    private AgentScopeConfig agentConfig;
    @Resource
    private ScriptKillTools scriptKillTools;

    // DM统一对话封装
    private String dmCall(String roomId, String prompt) {
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(prompt)
                .build();
        RuntimeContext ctx = RuntimeContext.builder().sessionId("room_" + roomId).build();
        String reply = dmAgent.call(List.of(msg), ctx).block().getTextContent();
        recordHistory(roomId, "【DM】" + reply);
        return reply;
    }

    // 1. 开局初始化房间数据
    public String startGame(String roomId, String humanRole) {
        roomHistory.put(roomId, new StringBuilder());
        roomHumanRole.put(roomId, humanRole);
        roomStage.put(roomId, GameStage.OPEN);
        roomIntroDone.put(roomId, ConcurrentHashMap.newKeySet());
        // 清空房间工具层缓存
        scriptKillTools.clearRoomAllData(roomId);

        String openPrompt = """
                你是剧本杀DM，只交代基础案件背景，不公布角色、不暴露凶手。
                剧情：庄园主张老爷死于书房密室，茶杯检出毒素。
                共三名嫌疑人，所有人完成自我介绍后进入搜证阶段。
                当前阶段仅引导自我介绍，不发放线索。
                """;
        String dmReply = dmCall(roomId, openPrompt);
        roomStage.put(roomId, GameStage.SELF_INTRO);
        return "游戏开局完成，当前阶段：自我介绍阶段\nDM：" + dmReply;
    }

    // 手动搜证接口（真人玩家调用）
    public String searchEvidence(String roomId, String playerName, String scene) {
        GameStage stage = roomStage.get(roomId);
        if (!GameStage.SEARCH.equals(stage)) {
            return "当前非搜证阶段，无法搜证";
        }
        String clueResult = scriptKillTools.searchClue(roomId, playerName, scene);
        String record = String.format("【手动搜证-%s】%s", playerName, clueResult);
        recordHistory(roomId, record);
        return record;
    }

    // 投票接口（真人调用）
    public String voteSuspect(String roomId, String voterName, String targetRole) {
        GameStage stage = roomStage.get(roomId);
        if (!GameStage.VOTE.equals(stage)) {
            return "当前非投票阶段，无法投票";
        }
        String voteResult = scriptKillTools.voteSuspect(roomId, voterName, targetRole);
        recordHistory(roomId, "【投票-" + voterName + "】" + voteResult);
        return voteResult;
    }

    // 角色自我介绍
    public String roleSelfIntro(String roomId, String roleName) {
        GameStage stage = roomStage.get(roomId);
        if (!GameStage.SELF_INTRO.equals(stage)) {
            return "当前非自我介绍阶段，无法自我介绍";
        }
        String[] roleInfo = ROLE_MAP.get(roleName);
        String publicIdentity = roleInfo[0];
        String secret = roleInfo[1];

        String introPrompt = String.format("""
                你是角色【%s】，身份：%s。
                自我介绍仅展示公开人设，严禁泄露隐藏秘密。
                简短自然一段自我介绍。
                """, roleName, publicIdentity);

        ReActAgent agent = agentConfig.createPlayerAgent(roleName, publicIdentity, secret);
        Msg msg = Msg.builder().role(MsgRole.USER).textContent(introPrompt).build();
        RuntimeContext ctx = RuntimeContext.builder().sessionId("room_" + roomId + "_" + roleName).build();
        String introText = agent.call(List.of(msg), ctx).block().getTextContent();

        roomIntroDone.get(roomId).add(roleName);
        recordHistory(roomId, String.format("【%s自我介绍】：%s", roleName, introText));

        // 三人全部自我介绍完毕，切换搜证阶段
        Set<String> doneSet = roomIntroDone.get(roomId);
        if (doneSet.size() == 3) {
            dmCall(roomId, """
                    全部自我介绍完成，进入搜证阶段！
                    每位玩家拥有一次搜证机会，可调用search_clue工具，传入roomId、自身角色名、场景名称获取线索。
                    可选场景：tea_room / study_room / maid_room / business_room / doctor_room
                    """);
            roomStage.put(roomId, GameStage.SEARCH);
        }
        return introText;
    }

    // AI自动执行一轮搜证（AI自主调用search_clue工具获取线索）
    public String aiAutoSearchAllOtherRole(String roomId) {
        GameStage stage = roomStage.get(roomId);
        if (!GameStage.SEARCH.equals(stage)) {
            return "当前非搜证阶段，AI无法自动搜证";
        }
        StringBuilder resultSb = new StringBuilder();
        String humanRole = roomHumanRole.get(roomId);
        String fullHistory = getRoomHistory(roomId);

        for (Map.Entry<String, String[]> entry : ROLE_MAP.entrySet()) {
            String aiRole = entry.getKey();
            if (aiRole.equals(humanRole)) continue;
            // 判断是否已搜证，未搜证才执行AI自动搜证
            if (scriptKillTools.playerHasSearched(roomId, aiRole)) {
                resultSb.append("【AI-").append(aiRole).append("】已完成搜证，跳过\n");
                continue;
            }

            ReActAgent aiAgent = agentConfig.createPlayerAgent(aiRole, entry.getValue()[0], entry.getValue()[1]);
            String prompt = String.format("""
                    当前处于搜证阶段，你是角色%s，房间ID:%s。
                    查看全局对话记录，自主选择一个场景调用search_clue工具搜证线索，每人只能搜证一次。
                    全局对话记录：%s
                    """, aiRole, roomId, fullHistory);

            Msg msg = Msg.builder().role(MsgRole.USER).textContent(prompt).build();
            RuntimeContext ctx = RuntimeContext.builder().sessionId("room_" + roomId + "_auto_search_" + aiRole).build();
            // ReAct自动识别并调用search_clue工具，返回包含搜证结果的完整输出
            String aiOutput = aiAgent.call(List.of(msg), ctx).block().getTextContent();

            String recordText = String.format("【AI自动搜证-%s】%s", aiRole, aiOutput);
            recordHistory(roomId, recordText);
            resultSb.append(recordText).append("\n");
        }
        return resultSb.toString();
    }

    // 真人玩家发言
    public String humanPlayerChat(String roomId, String content) {
        GameStage stage = roomStage.get(roomId);
        if (GameStage.SELF_INTRO.equals(stage)) {
            return "自我介绍阶段请先完成自我介绍再发言";
        }
        String humanRole = roomHumanRole.get(roomId);
        String[] roleInfo = ROLE_MAP.get(humanRole);
        String historyText = getRoomHistory(roomId);
        recordHistory(roomId, String.format("【真人玩家-%s】：%s", humanRole, content));

        ReActAgent humanAgent = agentConfig.createPlayerAgent(humanRole, roleInfo[0], roleInfo[1]);
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("全局全部对话+搜证记录：" + historyText + "\n我的发言：" + content + "\n结合线索推理，隐藏自身秘密")
                .build();
        RuntimeContext ctx = RuntimeContext.builder().sessionId("room_" + roomId + "_human").build();
        String reply = humanAgent.call(List.of(msg), ctx).block().getTextContent();
        recordHistory(roomId, String.format("【%s角色回应】：%s", humanRole, reply));
        return reply;
    }

    // 切换下一游戏阶段
    public String dmNextStage(String roomId) {
        GameStage current = roomStage.get(roomId);
        String prompt = switch (current) {
            case SEARCH -> """
                    搜证阶段结束，进入公聊推理阶段，所有人分享搜到的线索，互相质问梳理疑点。
                    """;
            case DISCUSS -> """
                    公聊结束，进入投票阶段，调用vote_suspect工具传入roomId、自己名字、嫌疑人完成投票。
                    """;
            case VOTE -> {
                String maxSuspect = scriptKillTools.getRoomMaxVoteSuspect(roomId);
                yield String.format("投票结束，最高票嫌疑人：%s，完整复盘案件全部真相、凶手作案手法与动机", maxSuspect);
            }
            default -> "无法切换阶段，当前阶段：" + current;
        };
        String dmReply = dmCall(roomId, prompt);
        switch (current) {
            case SEARCH -> roomStage.put(roomId, GameStage.DISCUSS);
            case DISCUSS -> roomStage.put(roomId, GameStage.VOTE);
            case VOTE -> roomStage.put(roomId, GameStage.REVIEW);
            default -> {}
        }
        return "切换新阶段：" + roomStage.get(roomId) + "\nDM：" + dmReply;
    }

    // 批量执行所有AI自我介绍
    public String aiAllSelfIntro(String roomId) {
        StringBuilder sb = new StringBuilder();
        String human = roomHumanRole.get(roomId);
        for (String role : ROLE_MAP.keySet()) {
            if (!role.equals(human)) {
                String intro = roleSelfIntro(roomId, role);
                sb.append("【AI-").append(role).append("自我介绍】：").append(intro).append("\n");
            }
        }
        return sb.toString();
    }

    // AI角色一轮自由公聊发言
    public String aiOtherRoleTalk(String roomId) {
        GameStage stage = roomStage.get(roomId);
        if (GameStage.SELF_INTRO.equals(stage)) {
            return "自我介绍阶段无法自由发言";
        }
        StringBuilder sb = new StringBuilder();
        String human = roomHumanRole.get(roomId);
        String historyText = getRoomHistory(roomId);

        for (Map.Entry<String, String[]> entry : ROLE_MAP.entrySet()) {
            String roleName = entry.getKey();
            if (roleName.equals(human)) continue;
            String identity = entry.getValue()[0];
            String secret = entry.getValue()[1];
            ReActAgent aiAgent = agentConfig.createPlayerAgent(roleName, identity, secret);

            Msg msg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent("完整对话与搜证线索记录：" + historyText + "，轮到你发言推理，隐藏自身秘密")
                    .build();
            RuntimeContext ctx = RuntimeContext.builder().sessionId("room_" + roomId + "_talk_" + roleName).build();
            String talk = aiAgent.call(List.of(msg), ctx).block().getTextContent();
            String record = String.format("【AI-%s】：%s", roleName, talk);
            recordHistory(roomId, record);
            sb.append(record).append("\n");
        }
        return sb.toString();
    }

    // 记录对话到房间历史
    private void recordHistory(String roomId, String text) {
        roomHistory.get(roomId).append(text).append("\n");
    }

    // 获取房间完整对话历史
    public String getRoomHistory(String roomId) {
        StringBuilder sb = roomHistory.get(roomId);
        return sb == null ? "房间不存在，请先开局" : sb.toString();
    }

    // 销毁房间，释放全部缓存，防止内存泄漏
    public void destroyRoom(String roomId) {
        roomHistory.remove(roomId);
        roomHumanRole.remove(roomId);
        roomStage.remove(roomId);
        roomIntroDone.remove(roomId);
        scriptKillTools.clearRoomAllData(roomId);
    }
}