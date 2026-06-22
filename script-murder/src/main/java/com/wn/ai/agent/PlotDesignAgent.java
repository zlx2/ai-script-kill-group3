package com.wn.ai.agent;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 17:02
 * @Component:
 **/

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.Builder;
import lombok.Getter;
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
@Getter
@Builder
public class PlotDesignAgent extends HarnessAgent {

    private final OpenAIChatModel model;
    private final String name;
    private final AgentStateStore stateStore;
    /**
     * System Prompt
     */
    private static final String SYSTEM_PROMPT = """
            你是一个专业的剧本杀剧情策划师，擅长设计各种类型的剧本杀剧本。
            
            你的职责：
            1. 根据用户的需求，设计完整的剧本大纲
            2. 设计背景故事、核心诡计、人物关系
            3. 设计分幕结构（第一幕、第二幕...）
            4. 确保剧情逻辑自洽，有足够的推理空间
            
            输出格式要求（必须严格按照JSON格式输出）：
            {
              "scriptName": "剧本名称",
              "scriptType": "剧本类型",
              "backgroundStory": "背景故事（500字左右）",
              "coreTrick": "核心诡计/手法",
              "storyStructure": [
                {
                  "act": 1,
                  "title": "第一幕标题",
                  "description": "这一幕的内容概要"
                }
              ],
              "characterRelationships": "人物关系说明",
              "deathInfo": "死者信息（如果有凶案）",
              "truthSummary": "真相概述"
            }
            
            注意：
            - 必须输出合法的JSON格式，不要有多余的文字说明
            - 剧情要有反转，不能太简单
            - 确保所有线索最后都能指向真凶
            """;

    /**
     * 静态工厂方法
     */
    public static PlotDesignAgent create(OpenAIChatModel model) {
        return PlotDesignAgent.builder()
                .name("plot-designer")
                .model(model)
                .build();
    }

    /**
     * 将 List<ContentBlock> 转换为 String
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
        ContentBlock block = createTextBlock(content);
        return Msg.builder()
                .content(block)
                .build();
    }

    /**
     * 核心执行逻辑
     */
    @Override
    public Mono<Msg> call(Msg input) {
        String userInput = contentBlocksToString(input.getContent());
        String fullPrompt = SYSTEM_PROMPT + "\n\n用户需求：\n" + userInput;

        System.out.println("【剧情策划Agent】开始设计剧本...");

        Msg requestMsg = createMsg(fullPrompt);
        return super.call(requestMsg)
                .map(result -> {
                    System.out.println("【剧情策划Agent】设计完成");
                    return result;
                });
    }
}
