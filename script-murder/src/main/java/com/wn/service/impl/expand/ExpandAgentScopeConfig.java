package com.wn.service.impl.expand;

import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.extensions.redis.state.RedisAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.lettuce.core.RedisClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI扩写代理配置类
 */
@Configuration
public class ExpandAgentScopeConfig {
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.password}")
    private String password;
    @Value("${spring.data.redis.port}")
    private String port;
    @Bean(name = "expandAgentModel")
    public OpenAIChatModel openAIChatModel() {
        return OpenAIChatModel.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))//看自己的环境变量
                .baseUrl("https://api.deepseek.com")//官网找
                .modelName("deepseek-chat")//官网找，这个好像7.24号就取消了，注意
                .generateOptions(GenerateOptions.builder()
                        .temperature(0.7)
                        .maxTokens(4096)
                        .build())
                .build();
    }

    @Bean
    public RedisAgentStateStore redisAgentStateStore() {
        RedisClient redisClient = RedisClient.create("redis://" + password + "@" + host + ":" + port);
        return RedisAgentStateStore.builder()
                .lettuceClient(redisClient)
                .build();
    }

    @Bean(name = "expandAgent")  // 给 Bean 起个名字
    public HarnessAgent harnessAgent(@Qualifier("expandAgentModel") OpenAIChatModel model, RedisAgentStateStore stateStore) {
        return HarnessAgent.builder()
                .name("ai-writer")
                .sysPrompt("你是一个剧本杀创作扩写助手。\n" +
                        "\n" +
                        "【最重要规则】\n" +
                        "1. 只输出扩写后的内容本身，不要加任何前缀（如\"好的\"、\"以下是\"、\"为你生成\"、\"这是\"等），不要加任何后缀（如\"希望对你有帮助\"、\"如果满意请\"等）\n" +
                        "2. 不要输出任何解释、评价、客套话，直接输出扩写结果\n" +
                        "3. 严格保持原文的语气和风格\n" +
                        "\n" +
                        "根据用户输入的创意碎片，按以下12个板块输出完整内容：\n" +
                        "\n" +
                        "1. 剧本简介 - 类型标签、人数时长、宣传导语（150字内）、特色亮点\n" +
                        "2. 背景故事 - 世界观、前史、案发当日氛围描写\n" +
                        "3. 分幕设计 - 每幕名称、DM开场白、环节目标、时长建议、幕间过渡\n" +
                        "4. 角色设计 - 每角色人设、性格、背景故事、秘密信息、隐藏动机\n" +
                        "5. 角色分幕剧本 - 每幕主内容（可阅读剧情）+ 提示内容（心理活动/演绎指引）\n" +
                        "6. 线索设计 - 编号、类型（公开/隐藏/搜查）、描述、推理方向、指向性\n" +
                        "7. DM扶车提示 - 卡点预判、引导话术、时间控制\n" +
                        "8. 完整复盘 - 时间线、作案全流程、证据链闭环\n" +
                        "9. 结局设计 - 正确结局真相、至少2种错误结局及破绽\n" +
                        "10. 首发解释 - 叙事手法说明（倒叙/双线/罗生门等）\n" +
                        "11. 作案动机 - 表层动机+深层动机+导火索\n" +
                        "12. 时间线扩写 - 全角色精确时间轴，精确到分钟\n" +
                        "\n" +
                        "规则：\n" +
                        "- 不用AI腔万能词，每段要有信息量\n" +
                        "- 每条线索必须有明确用途\n" +
                        "- 每幕必须有DM可读的开场白\n" +
                        "- 输出按序号顺序，不遗漏\n" +
                        "- 用户输入过简时，追问3个关键问题锁定方向（但仍只输出内容，不加前缀）")
                .model(model)
                .stateStore(stateStore)//存储方案
                .build();
    }
}
