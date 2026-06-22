package com.wn.ai.agent;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 17:32
 * @Component:
 **/

import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.state.AgentStateStore;

import io.agentscope.core.tool.Tool;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

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
@Getter
@Setter
@Builder
public class NpcAgent extends HarnessAgent {

    private final OpenAIChatModel model;
    private final String name;
    private final String npcName;           // NPC名称
    private final String npcPersonality;    // NPC性格描述
    private final String npcBackground;     // NPC背景故事
//    private final List<Tool> tools;         // 工具列表
    private final AgentStateStore stateStore;
    private String sessionId;

    /**
     * 静态工厂方法：创建NPC对话Agent
     *
     * 【AgentScope知识点12：动态创建Agent】
     * NPC对话这种场景，每个NPC有不同的人设，
     * 所以需要根据角色信息动态创建Agent，设置不同的System Prompt。
     */
    public static NpcAgent create(OpenAIChatModel model,
                                  String npcName,
                                  String npcPersonality,
                                  String npcBackground,
                                  List<Tool> tools) {
        return NpcAgent.builder()
                .name("npc-" + npcName)
                .model(model)
                .npcName(npcName)
                .npcPersonality(npcPersonality)
                .npcBackground(npcBackground)
//                .tools(tools)
                .build();
    }

    /**
     * 构建System Prompt（根据NPC信息动态生成）
     */
    private String buildSystemPrompt() {
        return String.format("""
                你是剧本杀游戏中的NPC角色【%s】。
                
                你的性格特点：%s
                
                你的背景故事：%s
                
                请严格按照你的角色设定和玩家对话，不要暴露你是AI。
                回答要符合你的身份和性格，不要说超出你角色认知范围的话。
                如果玩家问到你不知道的事情，可以说"我不知道"或者转移话题。
                保持对话自然、有趣，像真人一样交流。
                
                记住：你是NPC，不是主持人。你不知道剧本的完整真相，
                只知道你自己角色的信息和经历。
                """,
                npcName,
                npcPersonality != null ? npcPersonality : "待定",
                npcBackground != null ? npcBackground : "待定"
        );
    }

    /**
     * 将 ContentBlock 列表转换为字符串
     */
    private String contentBlocksToString(List<ContentBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }
        return blocks.stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .collect(Collectors.joining("\n"));
    }

    /**
     * 创建文本 ContentBlock
     */
    private ContentBlock createTextBlock(String text) {
        return TextBlock.builder()
                .text(text)
                .build();
    }

    /**
     * 创建 Msg
     */
    private Msg createMsg(String content) {
        return Msg.builder()
                .content(createTextBlock(content))
                .build();
    }

    /**
     * 核心执行逻辑：处理玩家对话
     */
    @Override
    public Mono<Msg> call(Msg input) {
        // 获取玩家消息
        String playerMessage = contentBlocksToString(input.getContent());

        // 构建完整Prompt：System Prompt + 对话历史 + 玩家消息
        String systemPrompt = buildSystemPrompt();
        String fullPrompt = systemPrompt + "\n\n玩家说：" + playerMessage + "\n\n请作为" + npcName + "回复玩家：";

        System.out.println("【" + npcName + "】收到玩家消息：" + playerMessage);

        Msg requestMsg = createMsg(fullPrompt);
        return super.call(requestMsg)
                .map(result -> {
                    String reply = contentBlocksToString(result.getContent());
                    System.out.println("【" + npcName + "】回复：" + reply);
                    return result;
                });
    }

    /**
     * 【AgentScope知识点23：流式对话】
     * NPC对话建议用流式输出，体验更好。
     * streamEvents()方法返回一个事件流，
     * 前端可以用SSE或者WebSocket接收，实现打字机效果。
     *
     * HarnessAgent 默认支持流式输出，直接调用即可。
     */
    public Flux<AgentEvent> streamEvents(Msg input) {
        String playerMessage = contentBlocksToString(input.getContent());
        String systemPrompt = buildSystemPrompt();
        String fullPrompt = systemPrompt + "\n\n玩家说：" + playerMessage + "\n\n请作为" + npcName + "回复玩家：";

        Msg requestMsg = createMsg(fullPrompt);

        // HarnessAgent 的 streamEvents 方法
        return super.streamEvents(requestMsg);
    }
}