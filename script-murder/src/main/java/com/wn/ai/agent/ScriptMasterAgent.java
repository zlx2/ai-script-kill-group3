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
 * 剧本生成总控Agent
 * 适配AgentScope 2.0.0-RC2 实际API
 */
@Component
@Getter

public class ScriptMasterAgent {


    private final HarnessAgent scriptMasterHarnessAgent;
    public ScriptMasterAgent(@Qualifier("scriptMasterHarnessAgent") HarnessAgent scriptMasterHarnessAgent) {
        this.scriptMasterHarnessAgent = scriptMasterHarnessAgent;
    }

    private static final String SYSTEM_PROMPT = """
            你是剧本杀总控策划，必须严格按照4步流程生成剧本：
            步骤1：根据用户需求生成完整剧本大纲；
            步骤2：拿到大纲后生成全部角色人设；
            步骤3：结合大纲、角色设计线索；
            步骤4：全部内容完成后校验逻辑；
            最后汇总四部分结果，输出完整结构化剧本。
            """;

    public Mono<Msg> call(Msg input) {
        String userRequirement = contentBlocksToString(input.getContent());
        System.out.println("【总控Agent】开始生成剧本，需求：" + userRequirement);

        String fullPrompt = SYSTEM_PROMPT + "\n\n用户需求：\n" + userRequirement;
        Msg requestMsg = createMsg(fullPrompt);

        return scriptMasterHarnessAgent.call(requestMsg)
                .map(result -> {
                    System.out.println("【总控Agent】剧本生成完成");
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
