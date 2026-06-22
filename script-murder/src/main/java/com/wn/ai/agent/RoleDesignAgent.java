package com.wn.ai.agent;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.Builder;
import lombok.Getter;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 17:24
 * @Component:
 **/

/**
 * 角色设计Agent
 * 负责：设计每个角色的背景、性格、秘密、人物关系
 */
@Getter
@Builder
public class RoleDesignAgent extends HarnessAgent {

    private final OpenAIChatModel model;
    private final String name;
    private final AgentStateStore stateStore;

    /**
     * System Prompt
     */
    private static final String SYSTEM_PROMPT = """
            你是一个专业的剧本杀角色设计师。
            
            你的职责：根据剧本大纲，设计每个角色的详细信息。
            
            每个角色需要包含：
            1. 角色姓名
            2. 性别、年龄
            3. 外貌描述
            4. 性格特点
            5. 背景故事（800字左右）
            6. 角色秘密（不能让别人知道的事情）
            7. 和其他角色的关系
            8. 角色动机（为什么有嫌疑）
            9. 专属线索
            
            输出格式（严格JSON）：
            {
              "roles": [
                {
                  "roleName": "角色名",
                  "gender": "男/女",
                  "age": 年龄,
                  "appearance": "外貌描述",
                  "personality": "性格特点",
                  "background": "背景故事",
                  "secret": "角色秘密",
                  "relationships": [
                    {"target": "对方角色名", "relation": "关系描述"}
                  ],
                  "motive": "杀人动机",
                  "isKiller": true/false
                }
              ]
            }
            
            注意：
            - 有且只有一个凶手
            - 每个角色都要有嫌疑，不能太明显
            - 角色之间要有复杂的关系网
            - 秘密要和主线剧情相关
            """;

    /**
     * 静态工厂方法
     */
    public static RoleDesignAgent create(OpenAIChatModel model,AgentStateStore stateStore) {
        return RoleDesignAgent.builder()
                .name("role-designer")
                .model(model)
                .stateStore(stateStore)
                .build();
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
     * 核心执行逻辑
     */
    @Override
    public Mono<Msg> call(Msg input) {
        // 获取用户输入（剧本大纲）
        String userInput = contentBlocksToString(input.getContent());
        String fullPrompt = SYSTEM_PROMPT + "\n\n剧本大纲：\n" + userInput;

        System.out.println("【角色设计Agent】开始设计角色...");

        Msg requestMsg = createMsg(fullPrompt);
        return super.call(requestMsg)
                .map(result -> {
                    System.out.println("【角色设计Agent】设计完成");
                    return result;
                });
    }
}
