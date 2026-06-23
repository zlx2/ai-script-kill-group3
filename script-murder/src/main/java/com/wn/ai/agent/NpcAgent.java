package com.wn.ai.agent;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 17:32
 * // @Component:
 **/

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * NPC对话Agent
 * 负责：扮演剧本中的NPC，和玩家进行对话
 *
 * 【AgentScope知识点22：会话状态管理】
 * 每个NPC对话Agent都有自己的会话状态，
 * 包括对话历史、上下文等，存在AgentStateStore里。
 *
 * 这样玩家和NPC的对话是有记忆的，
 * NPC记得之前聊过什么，不会重复问同样的问题。
 * 就像真人聊天一样，你说过的话我都记得。
 */
@Component
@Getter
public class NpcAgent {

    private final HarnessAgent npcHarnessAgent;

    public NpcAgent(@Qualifier("npcHarnessAgent") HarnessAgent npcHarnessAgent) {
        this.npcHarnessAgent = npcHarnessAgent;
    }

    private String npcName;
    private String npcPersonality;
    private String npcBackground;

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

    public Mono<Msg> call(Msg input) {
        String playerMessage = contentBlocksToString(input.getContent());
        String systemPrompt = buildSystemPrompt();
        String fullPrompt = systemPrompt + "\n\n玩家说：" + playerMessage + "\n\n请作为" + npcName + "回复玩家：";

        System.out.println("【" + npcName + "】收到玩家消息：" + playerMessage);

        Msg requestMsg = createMsg(fullPrompt);
        return npcHarnessAgent.call(requestMsg)
                .map(result -> {
                    String reply = contentBlocksToString(result.getContent());
                    System.out.println("【" + npcName + "】回复：" + reply);
                    return result;
                });
    }

    public Flux<AgentEvent> streamEvents(Msg input) {
        String playerMessage = contentBlocksToString(input.getContent());
        String systemPrompt = buildSystemPrompt();
        String fullPrompt = systemPrompt + "\n\n玩家说：" + playerMessage + "\n\n请作为" + npcName + "回复玩家：";

        Msg requestMsg = createMsg(fullPrompt);
        return npcHarnessAgent.streamEvents(requestMsg);
    }

    private String contentBlocksToString(List<ContentBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) return "";
        return blocks.stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).getText())
                .collect(Collectors.joining("\n"));
    }

    private ContentBlock createTextBlock(String text) {
        return TextBlock.builder().text(text).build();
    }

    private Msg createMsg(String content) {
        return Msg.builder().content(createTextBlock(content)).build();
    }
}