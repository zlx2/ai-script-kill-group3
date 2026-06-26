/**
 * @Author: 杜江
 * @Description: 主持人智能体
 * @DateTime: 2026/6/25 14:54
 * @Component:
 **/
package com.wn.agent;

import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class DmAgent {

    private final HarnessAgent dmHarnessAgent;
    @Setter
    private String scriptInfo;
    @Setter
    private String currentState;

    public DmAgent(HarnessAgent dmHarnessAgent) {
        this.dmHarnessAgent = dmHarnessAgent;
    }

    private String buildSystemPrompt() {
        return """
                你是一位专业的剧本杀DM（主持人），精通各种剧本杀流程和节奏把控。
                
                剧本信息：
                %s
                
                当前游戏状态：
                %s
                
                你的任务：
                1. 根据剧本信息和当前状态，决定下一步应该执行什么操作
                2. 操作必须是以下JSON格式之一：
                   - {"action": "advance_act", "reason": "原因"}
                   - {"action": "send_hint", "level": 1, "reason": "原因"}
                   - {"action": "start_voting", "reason": "原因"}
                   - {"action": "end_voting", "reason": "原因"}
                   - {"action": "show_ending", "isCorrect": true, "reason": "原因"}
                   - {"action": "show_review", "reason": "原因"}
                   - {"action": "show_questions", "reason": "显示当前剧本的所有题目"}
                   - {"action": "check_player_questions", "roleId": 123, "reason": "检查某个角色的题目"}
                   - {"action": "wait", "reason": "等待玩家操作"}
                3. 请严格按照JSON格式输出，不要包含其他文字
                """.formatted(
                scriptInfo != null ? scriptInfo : "暂无剧本信息",
                currentState != null ? currentState : "游戏未开始"
        );
    }

    public Flux<AgentEvent> streamEvents(Msg input) {
        String userMessage = contentBlocksToString(input.getContent());
        String systemPrompt = buildSystemPrompt();
        String fullPrompt = systemPrompt + "\n\n玩家情况：" + userMessage + "\n\n请决定下一步操作（JSON格式）：";

        Msg requestMsg = Msg.builder().content(TextBlock.builder().text(fullPrompt).build()).build();
        return dmHarnessAgent.streamEvents(requestMsg);
    }

    public String analyzeAndDecide(String gameContext) {
        String systemPrompt = buildSystemPrompt();
        String fullPrompt = systemPrompt + "\n\n游戏情况：" + gameContext + "\n\n请决定下一步操作（JSON格式）：";

        Msg msg = Msg.builder().content(TextBlock.builder().text(fullPrompt).build()).build();
        return dmHarnessAgent.call(msg)
                .map(result -> result.getContent().stream()
                        .filter(b -> b instanceof TextBlock)
                        .map(b -> ((TextBlock) b).getText())
                        .collect(Collectors.joining("\n")))
                .block();
    }

    private String contentBlocksToString(List<ContentBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) return "";
        return blocks.stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).getText())
                .collect(Collectors.joining("\n"));
    }
}
