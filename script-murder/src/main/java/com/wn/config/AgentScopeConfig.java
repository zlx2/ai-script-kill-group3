package com.wn.config;

import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.state.AgentStateStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 核心配置类
 */
@Configuration
public class AgentScopeConfig {

    @Value("${agentscope.model.deepseek.api-key}")
    private String apiKey;

    @Value("${agentscope.model.deepseek.base-url}")
    private String baseUrl;

    @Value("${agentscope.model.deepseek.model-name}")
    private String modelName;

    @Value("${agentscope.model.deepseek.temperature:0.7}")
    private Double temperature;

    @Value("${agentscope.model.deepseek.max-tokens:4096}")
    private Integer maxTokens;

    /**
     * 配置大模型
     *
     * 【修正】OpenAIChatModel 在 core 中，包路径是 io.agentscope.core.model
     * 根据源码，builder 支持：
     * - apiKey(String)
     * - baseUrl(String)
     * - modelName(String)
     * - stream(boolean)
     * - generateOptions(GenerateOptions)  ← 温度和 maxTokens 在这里设置
     * - proxy(ProxyConfig)
     */
    @Bean
    public OpenAIChatModel chatModel() {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .stream(true)  // 流式输出，默认 true
                .generateOptions(
                        GenerateOptions.builder()
                                .temperature(temperature)
                                .maxTokens(maxTokens)
                                .build()
                )
                .build();
    }

    /**
     * 配置Agent状态存储
     *
     * 【注意】JdbcAgentStateStore 可能不在 agentscope-core 中
     * 如果找不到，需要自己实现或用 Redis
     */
    @Bean
    public AgentStateStore agentStateStore() {
        // 开发测试环境：使用内存存储
        return new InMemoryAgentStateStore();

        // 生产环境：建议使用 Redis 实现（需要自行实现或引入对应依赖）
        // return new RedisAgentStateStore(redisTemplate);
    }

    /**
     * 配置Agent工厂
     */
    @Bean
    public AgentFactory agentFactory(OpenAIChatModel chatModel, AgentStateStore agentStateStore) {
        return new AgentFactory(chatModel, agentStateStore);
    }
}
