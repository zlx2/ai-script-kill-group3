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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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
    public Flux<String> autoRun(String roomId) {
        return Flux.create(sink -> {
            int maxIterations = 50;
            int iteration = 0;
            boolean gameEnded = false;

            while (iteration++ < maxIterations && !gameEnded) {
                try {
                    // 1. 构建上下文
                    String gameContext = buildGameContext(roomId);

                    // 2. AI 决策
                    String decision = analyzeGame(roomId, gameContext);
                    JSONObject decisionObj = JSON.parseObject(decision);
                    String action = decisionObj.getString("action");
                    String reason = decisionObj.getString("reason");

                    // 3. 执行决策
                    String result = executeDecision(roomId, decision);

                    // 4. 推送事件
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("iteration", iteration);
                    eventData.put("action", action);
                    eventData.put("reason", reason);
                    eventData.put("result", result);
                    eventData.put("currentState", buildCurrentState(roomId));
                    eventData.put("timestamp", System.currentTimeMillis());
                    eventData.put("waitingForPlayer", "wait".equals(action)); // 告诉前端是否需要等待

                    sink.next(JSON.toJSONString(eventData));
                    log.info("AI DM 第{}轮决策: action={}, reason={}", iteration, action, reason);

                    // 5. 终局判断
                    if ("show_ending".equals(action) || "show_review".equals(action)) {
                        log.info("AI DM 运行结束: game over, roomId={}", roomId);
                        gameEnded = true;
                        break;
                    }

                    // 6. 🔥 wait 时保持连接，等待玩家操作
                    if ("wait".equals(action)) {
                        log.info("AI DM 等待玩家操作, roomId={}", roomId);

                        // 🔥 推送一个"等待中"状态
                        Map<String, Object> waitData = new HashMap<>();
                        waitData.put("type", "waiting");
                        waitData.put("message", "等待玩家操作...");
                        waitData.put("timestamp", System.currentTimeMillis());
                        sink.next(JSON.toJSONString(waitData));

                        // 🔥 轮询检查状态变化，而不是直接结束
                        int checkCount = 0;
                        while (checkCount < 20) { // 最多等待 20 * 5 = 100 秒
                            try {
                                Thread.sleep(5000); // 每5秒检查一次
                                checkCount++;

                                // 检查是否有新变化（投票完成/玩家发言等）
                                String newContext = buildGameContext(roomId);
                                if (hasGameStateChanged(roomId, newContext)) {
                                    log.info("检测到游戏状态变化, roomId={}, 继续运行", roomId);
                                    break; // 退出内层循环，继续外层循环
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }

                        if (checkCount >= 20) {
                            // 超时，发送提示并继续
                            Map<String, Object> timeoutData = new HashMap<>();
                            timeoutData.put("type", "timeout");
                            timeoutData.put("message", "等待超时，继续检查...");
                            sink.next(JSON.toJSONString(timeoutData));
                        }

                        continue; // 继续外层循环
                    }

                    // 7. 非 wait 动作，等待后继续
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
        // 简单实现：比较投票数量是否变化
        DmRoomStatePO state = roomService.getRoomState(roomId);
        if (state == null) return false;

        // 检查投票状态变化
        List<RoomVotePO> votes = voteService.getVoteResults(roomId);
        // 如果是投票阶段且投票人数增加了，返回 true
        if (state.getIsVoting() == 1 && votes.size() > 0) {
            // 缓存上一次的投票数量
            String cacheKey = "dm:votes:" + roomId;
            Integer lastVoteCount = (Integer) redisTemplate.opsForValue().get(cacheKey);
            if (lastVoteCount != null && lastVoteCount != votes.size()) {
                redisTemplate.opsForValue().set(cacheKey, votes.size());
                return true;
            }
            if (lastVoteCount == null) {
                redisTemplate.opsForValue().set(cacheKey, votes.size());
            }
        }

        return false;
    }

    @Override
    public Flux<String> startGame(String roomId, Long scriptId) {
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
