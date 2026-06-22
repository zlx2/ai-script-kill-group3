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
 * 剧本生成总控Agent
 * 适配AgentScope 2.0.0-RC2 实际API
 */
@Getter
@Builder
public class ScriptMasterAgent extends HarnessAgent {

    private final OpenAIChatModel model;
    private final String name;
    private final AgentStateStore stateStore;

    // 子Agent的System Prompt常量
    private static final String PLOT_SYSTEM = "你是专业剧本杀剧情策划，根据用户需求生成完整案件故事、幕次大纲";
    private static final String ROLE_SYSTEM = "根据剧本大纲创作全部角色：身份、秘密、作案动机、人物关系";
    private static final String CLUE_SYSTEM = "结合剧本与角色设计物证、口供、隐藏深浅线索";
    private static final String LOGIC_SYSTEM = "校验剧本作案手法、时间线、人物动机是否存在逻辑矛盾，列出漏洞并给出修改建议";

    /**
     * 构造方法：创建总控Agent
     */
    public static ScriptMasterAgent create(OpenAIChatModel model,AgentStateStore stateStore) {
        return ScriptMasterAgent.builder()
                .name("script-master")
                .model(model)
                .stateStore(stateStore)
                .build();
    }

    /**
     * 将 List<ContentBlock> 转换为 String
     * 只提取 TextBlock 类型的文本内容
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
     * 创建文本类型的 ContentBlock
     * 使用 TextBlock.Builder（因为构造函数是 private）
     */
    private ContentBlock createTextBlock(String text) {
        return TextBlock.builder()
                .text(text)
                .build();
    }

    /**
     * 创建 Msg 对象（2.0.0-RC2 正确方式）
     */
    private Msg createMsg(String content) {
        ContentBlock block = createTextBlock(content);
        return Msg.builder()
                .content(block)
                .build();
    }

    /**
     * 核心执行逻辑：串行调用4个子Agent
     */
    @Override
    public Mono<Msg> call(Msg input) {
        // 获取用户原始需求（从 ContentBlock 中提取文本）
        String userRequirement = contentBlocksToString(input.getContent());
        System.out.println("【总控Agent】开始生成剧本，需求：" + userRequirement);

        // System Prompt 通过用户消息内容传递
        String fullSystemPrompt = """
                你是剧本杀总控策划，必须严格按照4步流程生成剧本：
                步骤1：调用剧情策划Agent，根据用户需求生成完整剧本大纲；
                步骤2：拿到大纲后调用角色设计Agent，生成全部角色人设；
                步骤3：结合大纲、角色调用线索设计Agent设计线索；
                步骤4：全部内容完成后调用逻辑校验Agent校验逻辑；
                最后汇总四部分结果，输出完整结构化剧本，禁止省略任何一步。
                """;

        // === 步骤1：剧情策划 ===
        String plotInputContent = PLOT_SYSTEM + "\n\n" + fullSystemPrompt + "\n\n用户需求：" + userRequirement;
        Msg plotInput = createMsg(plotInputContent);

        return super.call(plotInput)
                .flatMap(plotResult -> {
                    String plotContent = contentBlocksToString(plotResult.getContent());
                    System.out.println("【剧情策划】完成");

                    // === 步骤2：角色设计 ===
                    String roleInputContent = ROLE_SYSTEM + "\n\n剧本大纲：\n" + plotContent;
                    Msg roleInput = createMsg(roleInputContent);

                    return super.call(roleInput)
                            .flatMap(roleResult -> {
                                String roleContent = contentBlocksToString(roleResult.getContent());
                                System.out.println("【角色设计】完成");

                                // === 步骤3：线索设计 ===
                                String clueInputContent = CLUE_SYSTEM +
                                        "\n\n剧本大纲：\n" + plotContent +
                                        "\n\n角色设定：\n" + roleContent;
                                Msg clueInput = createMsg(clueInputContent);

                                return super.call(clueInput)
                                        .flatMap(clueResult -> {
                                            String clueContent = contentBlocksToString(clueResult.getContent());
                                            System.out.println("【线索设计】完成");

                                            // === 步骤4：逻辑校验 ===
                                            String logicInputContent = LOGIC_SYSTEM +
                                                    "\n\n剧本大纲：\n" + plotContent +
                                                    "\n\n角色设定：\n" + roleContent +
                                                    "\n\n线索设计：\n" + clueContent;
                                            Msg logicInput = createMsg(logicInputContent);

                                            return super.call(logicInput)
                                                    .flatMap(logicResult -> {
                                                        String logicContent = contentBlocksToString(logicResult.getContent());
                                                        System.out.println("【逻辑校验】完成");

                                                        // === 汇总最终结果 ===
                                                        String finalResult = buildFinalResult(
                                                                plotContent,
                                                                roleContent,
                                                                clueContent,
                                                                logicContent
                                                        );

                                                        Msg finalMsg = createMsg(finalResult);

                                                        System.out.println("【总控Agent】剧本生成全部完成！");
                                                        System.out.println("=== 完整剧本 ===\n" + finalResult);

                                                        return Mono.just(finalMsg);
                                                    });
                                        });
                            });
                });
    }

    /**
     * 汇总4个子Agent的输出，生成完整结构化剧本
     */
    private String buildFinalResult(String plot, String role, String clue, String logic) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== 完整剧本杀剧本 ==========\n\n");
        sb.append("【一、剧情大纲】\n").append(plot).append("\n\n");
        sb.append("【二、角色设定】\n").append(role).append("\n\n");
        sb.append("【三、线索设计】\n").append(clue).append("\n\n");
        sb.append("【四、逻辑校验】\n").append(logic).append("\n\n");
        sb.append("========== 剧本生成完成 ==========");
        return sb.toString();
    }
}
