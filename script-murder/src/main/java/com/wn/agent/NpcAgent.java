package com.wn.agent;

import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.Getter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: 杜江
 * @Description: NPC对话智能体
 * @DateTime: 2026/6/24 17:47
 * @Component:
 **/
@Getter
public class NpcAgent {

    private final HarnessAgent npcHarnessAgent;
    private String npcName;
    private String npcPersonality;
    private String npcBackground;

    public NpcAgent(HarnessAgent npcHarnessAgent) {
        this.npcHarnessAgent = npcHarnessAgent;
    }

    public void setNpcInfo(String npcName, String npcPersonality, String npcBackground) {
        this.npcName = npcName;
        this.npcPersonality = npcPersonality;
        this.npcBackground = npcBackground;
    }

    private String buildSystemPrompt() {
        return String.format("""
                你是剧本杀游戏中的NPC角色【%s】。
                性格特点：%s
                背景故事：%s
                请严格按照你的角色设定和玩家对话，不要暴露你是AI。
                """, npcName,
                npcPersonality != null ? npcPersonality : "待定",
                npcBackground != null ? npcBackground : "待定");
    }

    public Flux<AgentEvent> streamEvents(Msg input) {
        String playerMessage = contentBlocksToString(input.getContent());
        String systemPrompt = buildSystemPrompt();
        String fullPrompt = systemPrompt + "\n\n玩家说：" + playerMessage + "\n\n请作为" + npcName + "回复玩家：";

        Msg requestMsg = Msg.builder().content(TextBlock.builder().text(fullPrompt).build()).build();
        return npcHarnessAgent.streamEvents(requestMsg);
    }

    private String contentBlocksToString(List<ContentBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) return "";
        return blocks.stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).getText())
                .collect(Collectors.joining("\n"));
    }
}
