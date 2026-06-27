package com.wn.config;

import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiScriptConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Value("${spring.ai.dashscope.chat.options.model}")
    private String modelName;

    @Bean
    public OpenAIChatModel scriptGenModel() {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://api.deepseek.com")
                .modelName(modelName)
                .stream(true)
                .build();
    }

    @Bean
    public HarnessAgent plotDesignAgent(OpenAIChatModel scriptGenModel) {
        return HarnessAgent.builder()
                .name("plot-design")
                .sysPrompt("你是专业剧本杀剧情策划师。请根据用户需求设计完整剧本大纲。\n\n" +
                        "要求：\n1. 仅输出标准 JSON 字符串，不要添加任何解释文字\n" +
                        "2. 不要使用 ```json 和 ``` 包裹\n" +
                        "3. JSON 格式：{\"scriptName\": \"...\", \"scriptType\": \"...\", \"theme\": \"...\", " +
                        "\"outline\": \"...\", \"backgroundStory\": \"...\", \"coreTrick\": \"...\", " +
                        "\"storyStructure\": \"...\", \"characterRelationships\": \"...\", \"deathInfo\": \"...\", " +
                        "\"truthSummary\": \"...\", \"description\": \"...\", \"playerCount\": 0}\n" +
                        "4. 确保所有字段不为空")
                .model(scriptGenModel)
                .build();
    }

    @Bean
    public HarnessAgent roleDesignAgent(OpenAIChatModel scriptGenModel) {
        return HarnessAgent.builder()
                .name("role-design")
                .sysPrompt("你是专业剧本杀角色设计师。请根据剧本大纲设计角色信息。\n\n" +
                        "要求：\n1. 仅输出标准 JSON 字符串，不要添加任何解释文字\n" +
                        "2. 不要使用 ```json 和 ``` 包裹\n" +
                        "3. JSON 格式：{\"roles\": [{\"roleName\": \"...\", \"gender\": \"...\", " +
                        "\"age\": 0, \"characterStory\": \"...\", \"secretInfo\": \"...\"}]}\n" +
                        "4. 确保所有字段不为空")
                .model(scriptGenModel)
                .build();
    }

    @Bean
    public HarnessAgent npcDialogueAgent(OpenAIChatModel scriptGenModel) {
        return HarnessAgent.builder()
                .name("npc-dialogue")
                .sysPrompt("你是专业剧本杀NPC角色扮演者。请根据角色设定进行对话。\n\n" +
                        "要求：\n1. 严格按照角色设定进行对话，保持角色性格一致\n" +
                        "2. 回答要自然、生动，符合角色身份\n" +
                        "3. 不要跳出角色回答问题\n" +
                        "4. 不要添加任何解释文字，直接输出对话内容")
                .model(scriptGenModel)
                .build();
    }

    @Bean
    public HarnessAgent dmAgent(OpenAIChatModel scriptGenModel) {
        return HarnessAgent.builder()
                .name("dm-agent")
                .sysPrompt("你是剧本杀游戏的AI主持人(DM)。\n\n" +
                        "职责：\n" +
                        "1. 根据当前游戏阶段生成简短自然的主持人发言\n" +
                        "2. 帮助玩家理解当前阶段该做什么\n" +
                        "3. 不要替玩家推理\n" +
                        "4. 不要直接说出凶手、真相、隐藏动机、未公开线索\n" +
                        "5. 不要擅自改变游戏状态\n" +
                        "6. 输出仅限JSON格式，不要输出多余文本")
                .model(scriptGenModel)
                .build();
    }
}
