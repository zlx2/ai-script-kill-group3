package com.wn.config;


import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 杜江
 * @Description:AI剧本杀Agent配置类，注入大模型与剧情、角色双智能体Bean
 * @DateTime: 2026/6/24 10:48
 * @Component:
 **/
@Configuration
public class AiScriptConfig {

    @Value("${agentscope.deepseek.api-key}")
    private String apiKey;

    @Value("${agentscope.deepseek.base-url}")
    private String baseUrl;

    @Value("${agentscope.deepseek.model-name}")
    private String modelName;

    @Bean
    public OpenAIChatModel scriptGenModel() {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .stream(true)
                .build();
    }

    @Bean
    public HarnessAgent plotDesignAgent(OpenAIChatModel scriptGenModel) {
        return HarnessAgent.builder()
                .name("plot-design")
                .sysPrompt("你是专业剧本杀剧情策划师。请根据用户需求设计完整剧本大纲。\n\n" +
                        "要求：\n" +
                        "1. 仅输出标准 JSON 字符串，不要添加任何解释文字\n" +
                        "2. 不要使用 ```json 和 ``` 包裹\n" +
                        "3. JSON 格式：{\"scriptName\": \"...\", \"scriptType\": \"...\", \"theme\": \"...\", \"outline\": \"...\", \"backgroundStory\": \"...\", \"coreTrick\": \"...\", \"storyStructure\": \"...\", \"characterRelationships\": \"...\", \"deathInfo\": \"...\", \"truthSummary\": \"...\", \"description\": \"...\", \"playerCount\": 0}\n" +
                        "4. 字段说明：\n" +
                        "   - scriptName: 剧本名称\n" +
                        "   - scriptType: 剧本类型（如：本格推理、变格推理、情感本、欢乐本）\n" +
                        "   - theme: 剧本主题\n" +
                        "   - outline: 剧本大纲（完整的故事梗概）\n" +
                        "   - backgroundStory: 背景故事（详细描述剧本发生的时代、地点、背景）\n" +
                        "   - coreTrick: 核心诡计（凶手的作案手法、关键线索）\n" +
                        "   - storyStructure: 故事结构（分幕设计）\n" +
                        "   - characterRelationships: 人物关系\n" +
                        "   - deathInfo: 死亡信息\n" +
                        "   - truthSummary: 真相总结\n" +
                        "   - description: 剧本描述\n" +
                        "   - playerCount: 玩家人数（整数）\n" +
                        "5. 确保所有字段不为空")
                .model(scriptGenModel)
                .build();
    }

    @Bean
    public HarnessAgent roleDesignAgent(OpenAIChatModel scriptGenModel) {
        return HarnessAgent.builder()
                .name("role-design")
                .sysPrompt("你是专业剧本杀角色设计师。请根据剧本大纲设计角色信息。\n\n" +
                        "要求：\n" +
                        "1. 仅输出标准 JSON 字符串，不要添加任何解释文字\n" +
                        "2. 不要使用 ```json 和 ``` 包裹\n" +
                        "3. JSON 格式：{\"roles\": [{\"roleName\": \"...\", \"gender\": \"...\", \"age\": 0, \"characterStory\": \"...\", \"secretInfo\": \"...\"}]}\n" +
                        "4. 确保所有字段不为空")
                .model(scriptGenModel)
                .build();
    }

    @Bean
    public HarnessAgent npcDialogueAgent(OpenAIChatModel scriptGenModel) {
        return HarnessAgent.builder()
                .name("npc-dialogue")
                .sysPrompt("你是专业剧本杀NPC角色扮演者。请根据角色设定进行对话。\n\n" +
                        "要求：\n" +
                        "1. 严格按照角色设定进行对话，保持角色性格一致\n" +
                        "2. 回答要自然、生动，符合角色身份\n" +
                        "3. 不要跳出角色回答问题\n" +
                        "4. 不要添加任何解释文字，直接输出对话内容")
                .model(scriptGenModel)
                .build();
    }
}
