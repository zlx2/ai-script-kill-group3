package com.wn.ai.agent;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 17:24
 * // @Component:
 **/

/**
 * 角色设计Agent
 * 负责：设计每个角色的背景、性格、秘密、人物关系
 */
@Component
@Getter

public class RoleDesignAgent {

    private final HarnessAgent roleDesignHarnessAgent;
    public RoleDesignAgent(@Qualifier("roleDesignHarnessAgent") HarnessAgent roleDesignHarnessAgent) {
        this.roleDesignHarnessAgent = roleDesignHarnessAgent;
    }

    private static final String SYSTEM_PROMPT = """
            你是一个专业的剧本杀角色设计师。
            职责：根据剧本大纲设计每个角色的详细信息（姓名、性别、年龄、性格、背景、秘密、关系、动机）。
            输出格式（JSON）：{roles: [{roleName, gender, age, appearance, personality, background, secret, relationships, motive, isKiller}]}
            """;

    public Mono<Msg> call(Msg input) {
        String userInput = contentBlocksToString(input.getContent());
        String fullPrompt = SYSTEM_PROMPT + "\n\n剧本大纲：\n" + userInput;

        System.out.println("【角色设计Agent】开始设计角色...");

        Msg requestMsg = createMsg(fullPrompt);
        return roleDesignHarnessAgent.call(requestMsg)
                .map(result -> {
                    System.out.println("【角色设计Agent】设计完成");
                    return result;
                });
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
