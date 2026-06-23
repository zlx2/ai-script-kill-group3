package com.wn.config;

import com.wn.ai.agent.NpcAgent;
import com.wn.ai.agent.PlotDesignAgent;
import com.wn.ai.agent.RoleDesignAgent;
import com.wn.ai.agent.ScriptMasterAgent;
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

    @Value("${agentscope.deepseek.api-key}")
    private String apiKey;

    @Value("${agentscope.deepseek.base-url}")
    private String baseUrl;

    @Value("${agentscope.deepseek.model-name}")
    private String modelName;

    @Value("${agentscope.deepseek.temperature:0.7}")
    private Double temperature;

    @Value("${agentscope.deepseek.max-tokens:4096}")
    private Integer maxTokens;

    /**
     * 配置大模型
     */
    @Bean
    public OpenAIChatModel chatModel() {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .stream(true)
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
     */
    @Bean
    public AgentStateStore agentStateStore() {
        return new InMemoryAgentStateStore();
    }

    /**
     * 配置Agent工厂
     */
    @Bean
    public AgentFactory agentFactory(ScriptMasterAgent scriptMasterAgent,
                                     PlotDesignAgent plotDesignAgent,
                                     RoleDesignAgent roleDesignAgent,
                                     NpcAgent npcAgent) {
        return new AgentFactory(scriptMasterAgent, plotDesignAgent, roleDesignAgent, npcAgent);
    }
}
