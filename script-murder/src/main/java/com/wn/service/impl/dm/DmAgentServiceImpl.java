/**
 * @Author: 杜江
 * @Description: 主持人智能体服务实现
 * @DateTime: 2026/6/25 14:58
 * @Component:
 **/
package com.wn.service.impl.dm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wn.agent.DmAgent;
import com.wn.entity.dm.*;
import com.wn.entity.script.stage.ScriptStagePO;
import com.wn.service.dm.*;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DmAgentServiceImpl implements DmAgentService {

    @Resource
    private HarnessAgent dmAgent;

    @Resource
    private DmScriptService scriptService;

    @Resource
    private DmRoomService roomService;

    @Resource
    private DmActService actService;

    @Resource
    private DmVoteService voteService;

    private final Map<String, DmAgent> agentMap = new HashMap<>();

    @Override
    public void initAgent(String roomId, Long scriptId) {
        DmAgent agent = getOrCreateAgent(roomId);

        String scriptInfo = buildScriptInfo(scriptId);
        agent.setScriptInfo(scriptInfo);

        String currentState = buildCurrentState(roomId);
        agent.setCurrentState(currentState);

        log.info("AI DM Agent初始化完成, roomId={}, scriptId={}", roomId, scriptId);
    }

    @Override
    public String analyzeGame(String roomId, String gameContext) {
        DmAgent agent = getOrCreateAgent(roomId);

        String currentState = buildCurrentState(roomId);
        agent.setCurrentState(currentState);

        String decision = agent.analyzeAndDecide(gameContext);
        log.info("AI DM决策结果, roomId={}, decision={}", roomId, decision);

        return decision;
    }

    @Override
    public String executeDecision(String roomId, String decisionJson) {
        try {
            JSONObject decision = JSON.parseObject(decisionJson);
            String action = decision.getString("action");
            String reason = decision.getString("reason");

            return switch (action) {
                case "advance_act" -> {
                    int newAct = actService.advanceAct(roomId);
                    yield "已推进到第" + newAct + "幕 - " + reason;
                }
                case "grant_clue", "grant_clue_to_all" -> {
                    yield "请玩家自行搜索线索 - " + reason;
                }
                case "send_hint" -> {
                    Byte level = decision.getByte("level");
                    yield "AI建议发送" + getHintLevelDesc(level) + "提示 - " + reason;
                }
                case "start_voting" -> {
                    voteService.startVoting(roomId);
                    yield "已开启投票 - " + reason;
                }
                case "end_voting" -> {
                    voteService.endVoting(roomId);
                    yield "已结束投票 - " + reason;
                }
                case "show_ending" -> {
                    boolean isCorrect = decision.getBooleanValue("isCorrect");
                    Long scriptId = getScriptIdByRoomId(roomId);
                    ScriptReviewPO review = scriptService.getScriptReview(scriptId);
                    String ending = isCorrect ? review.getCorrectEnding() : review.getWrongEnding();
                    yield (isCorrect ? "正确结局：" : "错误结局：") + ending;
                }
                case "show_review" -> {
                    Long scriptId = getScriptIdByRoomId(roomId);
                    scriptService.getScriptReview(scriptId);
                    yield "已发布复盘 - " + reason;
                }
                case "wait" -> "等待中 - " + reason;
                default -> "未知操作: " + action;
            };
        } catch (Exception e) {
            log.error("执行AI决策失败", e);
            return "执行失败: " + e.getMessage();
        }
    }

    @Override
    public String autoRun(String roomId) {
        StringBuilder log = new StringBuilder();

        // 循环执行直到游戏结束
        while (true) {
            String gameContext = buildGameContext(roomId);
            String decision = analyzeGame(roomId, gameContext);
            String result = executeDecision(roomId, decision);

            log.append(result).append("\n");

            // 如果是结局或等待，停止循环
            JSONObject decisionObj = JSON.parseObject(decision);
            String action = decisionObj.getString("action");
            if ("show_ending".equals(action) || "show_review".equals(action) || "wait".equals(action)) {
                break;
            }

            // 等待一段时间再继续
            try { Thread.sleep(10000); } catch (InterruptedException e) { break; }
        }

        return log.toString();
    }

    @Override
    public String startGame(String roomId, Long scriptId) {
        initAgent(roomId, scriptId);
        return autoRun(roomId);
    }

    private DmAgent getOrCreateAgent(String roomId) {
        return agentMap.computeIfAbsent(roomId, k -> {
            DmAgent agent = new DmAgent(dmAgent);
            log.info("创建AI DM Agent, roomId={}", roomId);
            return agent;
        });
    }

    private String buildScriptInfo(Long scriptId) {
        StringBuilder sb = new StringBuilder();

        List<ScriptStagePO> stages = scriptService.getScriptStages(scriptId);
        sb.append("=== 幕结构 ===\n");
        stages.forEach(stage -> sb.append(stage.getStageNo()).append(". ")
                .append(stage.getStageName()).append("\n"));

        sb.append("\n=== 线索库 === 玩家自主搜索\n");

        List<ScriptHintPO> hints = scriptService.getScriptHints(scriptId);
        sb.append("\n=== 扶车提示 ===").append(hints.size()).append("条\n");
        hints.forEach(hint -> sb.append("等级").append(hint.getHintLevel())
                .append(": ").append(hint.getHintContent()).append("\n"));

        ScriptReviewPO review = scriptService.getScriptReview(scriptId);
        if (review != null) {
            sb.append("\n=== 复盘信息 ===\n");
            sb.append("凶手角色ID: ").append(review.getMurdererRoleId()).append("\n");
        }

        return sb.toString();
    }

    private String buildCurrentState(String roomId) {
        DmRoomStatePO state = roomService.getRoomState(roomId);
        if (state == null) return "房间状态未初始化";

        StringBuilder sb = new StringBuilder();
        sb.append("当前幕数: ").append(state.getCurrentAct()).append("\n");
        sb.append("私聊状态: ").append(state.getIsPrivateChatEnabled() == 1 ? "开启" : "关闭").append("\n");
        sb.append("AI对话: ").append(state.getIsAiTalkEnabled() == 1 ? "开启" : "关闭").append("\n");
        sb.append("搜证轮数: ").append(state.getSearchRoundCount()).append("\n");
        sb.append("聊天时长: ").append(state.getChatDurationMinutes()).append("分钟\n");
        sb.append("投票状态: ").append(state.getIsVoting() == 1 ? "进行中" : "未开始").append("\n");

        return sb.toString();
    }

    private String buildGameContext(String roomId) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildCurrentState(roomId));

        sb.append("\n线索状态：玩家自主搜索\n");

        List<RoomVotePO> votes = voteService.getVoteResults(roomId);
        sb.append("已投票人数: ").append(votes.size()).append("\n");

        return sb.toString();
    }

    private String getHintLevelDesc(Byte level) {
        return switch (level) {
            case 1 -> "轻度";
            case 2 -> "中度";
            case 3 -> "重度";
            default -> "未知";
        };
    }

    private Long getScriptIdByRoomId(String roomId) {
        DmRoomStatePO state = roomService.getRoomState(roomId);
        return state != null ? state.getScriptId() : 1L;
    }
}
