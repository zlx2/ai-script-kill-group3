package com.wn.ai.agent;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 17:02
 * @Component:
 **/

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
 * 剧情策划Agent
 * 负责：生成剧本大纲、背景故事、核心诡计、分幕设计
 *
 * 【AgentScope知识点18：System Prompt - 给Agent设定人设】
 * 每个Agent都有一个System Prompt，用来定义它的角色、职责、输出格式等。
 * 这是Prompt Engineering的核心，写得好不好直接影响Agent的输出质量。
 *
 * 就像招聘员工，你得先写清楚岗位说明书，人家才知道要干什么。
 */
/**
 * 剧情策划Agent
 * 负责：生成剧本大纲、背景故事、核心诡计、分幕设计
 */
@Component
@Getter
@RequiredArgsConstructor
public class PlotDesignAgent {

    @Qualifier("plotDesignHarnessAgent")
    private final HarnessAgent plotDesignHarnessAgent;

    private static final String SYSTEM_PROMPT = """
            你是一个专业的剧本杀剧情策划师，擅长设计各种类型的剧本杀剧本。
            职责：根据用户需求设计完整剧本大纲、背景故事、核心诡计、分幕设计。
            输出格式要求（JSON）：{scriptName, scriptType, backgroundStory, coreTrick, storyStructure, characterRelationships, deathInfo, truthSummary}
            """;

    public Mono<Msg> call(Msg input) {
        String userInput = contentBlocksToString(input.getContent());
        String fullPrompt = SYSTEM_PROMPT + "\n\n用户需求：\n" + userInput;

        System.out.println("【剧情策划Agent】开始设计剧本...");

        Msg requestMsg = createMsg(fullPrompt);
        return plotDesignHarnessAgent.call(requestMsg)
                .map(result -> {
                    System.out.println("【剧情策划Agent】设计完成");
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
