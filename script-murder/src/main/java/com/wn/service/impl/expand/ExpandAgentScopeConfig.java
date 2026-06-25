package com.wn.service.impl.expand;

import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.extensions.redis.state.RedisAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.lettuce.core.RedisClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI扩写代理配置类
 */
@Configuration
public class ExpandAgentScopeConfig {
    @Bean(name = "expandAgentModel")
    public OpenAIChatModel openAIChatModel() {
        return OpenAIChatModel.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .baseUrl("https://api.deepseek.com")
                .modelName("deepseek-chat")
                .generateOptions(GenerateOptions.builder()
                        .temperature(0.7)
                        .maxTokens(4096)
                        .build())
                .build();
    }

    @Bean
    public RedisAgentStateStore redisAgentStateStore() {
        RedisClient redisClient = RedisClient.create("redis://woniuxy@192.168.120.12:6379");
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
                .stateStore(stateStore)
                .build();
    }
}
