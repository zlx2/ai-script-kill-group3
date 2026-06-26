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
                .sysPrompt("你是一个专业的文案扩写助手。请根据用户输入，提供生动、逻辑清晰的扩写内容。")
                .model(model)
                .stateStore(stateStore)//存储方案
                .build();
    }
}
