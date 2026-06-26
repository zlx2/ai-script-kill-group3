/**
 * @Author: 杜江
 * @Description: 主持人智能体服务实现
 * @DateTime: 2026/6/25 14:58
 * @Component:
 **/
package com.wn.service.impl.dm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wn.entity.script.ScriptPO;
import com.wn.service.script.ScriptService;
import com.wn.websocket.WebSocketHandler;
import com.wn.websocket.vo.WsMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wn.agent.DmAgent;
import com.wn.entity.R;
import com.wn.entity.dm.DmRoomStatePO;
import com.wn.entity.dm.RoomVotePO;
import com.wn.entity.dm.ScriptHintPO;
import com.wn.entity.dm.ScriptReviewPO;
import com.wn.entity.script.stage.ScriptStagePO;
import com.wn.service.dm.DmActService;
import com.wn.service.dm.DmAgentService;
import com.wn.service.dm.DmRoomService;
import com.wn.service.dm.DmScriptService;
import com.wn.service.dm.DmVoteService;
import com.wn.service.script.GameQuestionService;

import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

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

    @Resource
    private WebSocketHandler webSocketHandler;

    private final Map<String, DmAgent> agentMap = new HashMap<>();

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private GameQuestionService questionService;

    @Resource
    private ScriptService scriptInfoService;

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
                    broadcastToRoom(roomId, "act_change", Map.of(
                            "act", newAct,
                            "message", "已推进到第" + newAct + "幕"
                    ));
                    yield "已推进到第" + newAct + "幕 - " + reason;
                }
                case "grant_clue", "grant_clue_to_all" -> {
                    yield "请玩家自行搜索线索 - " + reason;
                }
                case "send_hint" -> {
                    Byte level = decision.getByte("level");
                    Long scriptId = getScriptIdByRoomId(roomId);
                    List<ScriptHintPO> hints = scriptService.getScriptHintsByLevel(scriptId, level);
                    if (!hints.isEmpty()) {
                        String hintContent = hints.get(0).getHintContent();
                        broadcastToRoom(roomId, "hint", Map.of(
                                "level", level,
                                "levelDesc", getHintLevelDesc(level),
                                "content", hintContent
                        ));
                        yield "已发送" + getHintLevelDesc(level) + "提示: " + hintContent;
                    } else {
                        yield "没有找到" + getHintLevelDesc(level) + "提示";
                    }
                }
                case "start_voting" -> {
                    voteService.startVoting(roomId);
                    broadcastToRoom(roomId, "voting_start", Map.of(
                            "message", "投票已开启，请投出你认为的凶手"
                    ));
                    yield "已开启投票 - " + reason;
                }
                case "end_voting" -> {
                    voteService.endVoting(roomId);
                    broadcastToRoom(roomId, "voting_end", Map.of(
                            "message", "投票已结束"
                    ));
                    yield "已结束投票 - " + reason;
                }
                case "show_ending" -> {
                    boolean isCorrect = decision.getBooleanValue("isCorrect");
                    Long scriptId = getScriptIdByRoomId(roomId);
                    ScriptReviewPO review = scriptService.getScriptReview(scriptId);
                    String ending = isCorrect ? review.getCorrectEnding() : review.getWrongEnding();
                    broadcastToRoom(roomId, "ending", Map.of(
                            "isCorrect", isCorrect,
                            "ending", ending
                    ));
                    yield (isCorrect ? "正确结局：" : "错误结局：") + ending;
                }
                case "show_review" -> {
                    Long scriptId = getScriptIdByRoomId(roomId);
                    ScriptReviewPO review = scriptService.getScriptReview(scriptId);
                    if (review != null) {
                        broadcastToRoom(roomId, "review", Map.of(
                                "murdererRoleId", review.getMurdererRoleId(),
                                "fullReview", review.getFullReview(),
                                "trickExplanation", review.getTrickExplanation(),
                                "timeline", review.getTimeline(),
                                "motivation", review.getMotivation()
                        ));
                        yield "已发布复盘";
                    } else {
                        yield "该剧本没有复盘信息";
                    }
                }
                case "show_questions" -> {
                    Long scriptId = getScriptIdByRoomId(roomId);
                    R result = questionService.listAllQuestionByScript(scriptId);
                    yield "已显示当前剧本的题目列表";
                }
                case "check_player_questions" -> {
                    Long scriptId = getScriptIdByRoomId(roomId);
                    Long roleId = decision.getLong("roleId");
                    R result = questionService.getQuestionByRole(scriptId, roleId);
                    yield "已查看角色" + roleId + "的题目";
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
    public Flux<String> autoRun(String roomId) {
        return Flux.create(sink -> {
            int maxIterations = 50;
            int iteration = 0;
            boolean gameEnded = false;

            while (iteration++ < maxIterations && !gameEnded) {
                try {
                    String gameContext = buildGameContext(roomId);

                    String decision = analyzeGame(roomId, gameContext);
                    JSONObject decisionObj = JSON.parseObject(decision);
                    String action = decisionObj.getString("action");
                    String reason = decisionObj.getString("reason");

                    String result = executeDecision(roomId, decision);

                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("iteration", iteration);
                    eventData.put("action", action);
                    eventData.put("reason", reason);
                    eventData.put("result", result);
                    eventData.put("currentState", buildCurrentState(roomId));
                    eventData.put("timestamp", System.currentTimeMillis());
                    eventData.put("waitingForPlayer", "wait".equals(action));

                    sink.next(JSON.toJSONString(eventData));
                    log.info("AI DM 第{}轮决策: action={}, reason={}", iteration, action, reason);

                    if ("show_ending".equals(action) || "show_review".equals(action)) {
                        log.info("AI DM 运行结束: game over, roomId={}", roomId);
                        gameEnded = true;
                        break;
                    }

                    if ("wait".equals(action)) {
                        log.info("AI DM 等待玩家操作, roomId={}", roomId);

                        Map<String, Object> waitData = new HashMap<>();
                        waitData.put("type", "waiting");
                        waitData.put("message", "等待玩家操作...");
                        waitData.put("timestamp", System.currentTimeMillis());
                        sink.next(JSON.toJSONString(waitData));

                        int checkCount = 0;
                        while (checkCount < 20) {
                            try {
                                Thread.sleep(5000);
                                checkCount++;

                                String newContext = buildGameContext(roomId);
                                if (hasGameStateChanged(roomId, newContext)) {
                                    log.info("检测到游戏状态变化, roomId={}, 继续运行", roomId);
                                    break;
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }

                        if (checkCount >= 20) {
                            Map<String, Object> timeoutData = new HashMap<>();
                            timeoutData.put("type", "timeout");
                            timeoutData.put("message", "等待超时，继续检查...");
                            sink.next(JSON.toJSONString(timeoutData));
                        }

                        continue;
                    }

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                } catch (Exception e) {
                    log.error("AI DM 运行异常", e);
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("error", e.getMessage());
                    errorData.put("iteration", iteration);
                    sink.next(JSON.toJSONString(errorData));
                    break;
                }
            }

            sink.complete();
            log.info("AI DM 自动运行结束, roomId={}", roomId);
        });
    }

    private boolean hasGameStateChanged(String roomId, String newContext) {
        DmRoomStatePO state = roomService.getRoomState(roomId);
        if (state == null) return false;

        List<RoomVotePO> votes = voteService.getVoteResults(roomId);
        String cacheKey = "dm:votes:" + roomId;
        Integer lastVoteCount = (Integer) redisTemplate.opsForValue().get(cacheKey);

        if (lastVoteCount != null && lastVoteCount != votes.size()) {
            redisTemplate.opsForValue().set(cacheKey, votes.size());
            return true;
        }
        if (lastVoteCount == null && votes.size() > 0) {
            redisTemplate.opsForValue().set(cacheKey, votes.size());
        }

        String actCacheKey = "dm:act:" + roomId;
        Integer lastAct = (Integer) redisTemplate.opsForValue().get(actCacheKey);
        if (lastAct != null && lastAct != state.getCurrentAct()) {
            redisTemplate.opsForValue().set(actCacheKey, state.getCurrentAct());
            return true;
        }
        if (lastAct == null) {
            redisTemplate.opsForValue().set(actCacheKey, state.getCurrentAct());
        }

        return false;
    }

    @Override
    public Flux<String> startGame(String roomId, Long scriptId) {
        initAgent(roomId, scriptId);

        ScriptPO script = scriptInfoService.getById(scriptId);
        String scriptName = script != null ? script.getScriptName() : "未知剧本";

        broadcastToRoom(roomId, "welcome", Map.of(
                "message", "🎭 欢迎来到《" + scriptName + "》剧本杀！\n\n请各位玩家阅读自己的角色剧本，准备开始游戏。\n祝大家玩得开心！",
                "scriptId", scriptId,
                "scriptName", scriptName
        ));

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

        R questionsResult = questionService.listAllQuestionByScript(scriptId);
        List<?> questions = (List<?>) questionsResult.get("data");
        sb.append("\n=== 题目列表 ===").append(questions.size()).append("题\n");
        questions.forEach(q -> {
            try {
                JSONObject qObj = JSON.parseObject(JSON.toJSONString(q));
                sb.append("- ").append(qObj.getString("questionTitle"))
                        .append("(角色ID:").append(qObj.getLong("roleId")).append(")\n");
            } catch (Exception e) {
                sb.append("- ").append(q).append("\n");
            }
        });

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

    private void broadcastToRoom(String roomId, String type, Map<String, Object> data) {
        try {
            Map<String, Object> messageData = new HashMap<>(data);
            messageData.put("type", type);
            messageData.put("timestamp", System.currentTimeMillis());

            WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                    .type(type)
                    .data(messageData)
                    .timestamp(System.currentTimeMillis())
                    .build();

            webSocketHandler.broadcastToRoom(roomId, message);
            log.info("AI DM 广播消息: type={}, roomId={}", type, roomId);
        } catch (Exception e) {
            log.error("AI DM 广播失败", e);
        }
    }
}
