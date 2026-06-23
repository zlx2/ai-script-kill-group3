package com.wn.config;

import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/23 09:38
 * @Component:
 **/
@Configuration
public class HarnessAgentConfig {

    @Bean
    public HarnessAgent scriptMasterHarnessAgent(OpenAIChatModel model, @Qualifier("agentStateStore") AgentStateStore stateStore) {
        return HarnessAgent.builder()
                .name("script-master")
                .sysPrompt("你是剧本杀总控策划，按照4步流程生成剧本")
                .model(model)
                .stateStore(stateStore)
                .build();
    }

    @Bean
    public HarnessAgent plotDesignHarnessAgent(OpenAIChatModel model, @Qualifier("agentStateStore") AgentStateStore stateStore) {
        return HarnessAgent.builder()
                .name("plot-designer")
                .sysPrompt("你是专业剧本杀剧情策划师，设计剧本大纲")
                .model(model)
                .stateStore(stateStore)
                .build();
    }

    @Bean
    public HarnessAgent roleDesignHarnessAgent(OpenAIChatModel model, @Qualifier("agentStateStore") AgentStateStore stateStore) {
        return HarnessAgent.builder()
                .name("role-designer")
                .sysPrompt("你是专业剧本杀角色设计师，设计角色信息")
                .model(model)
                .stateStore(stateStore)
                .build();
    }

    @Bean
    public HarnessAgent npcHarnessAgent(OpenAIChatModel model, @Qualifier("agentStateStore") AgentStateStore stateStore) {
        return HarnessAgent.builder()
                .name("npc-agent")
                .sysPrompt("你是剧本杀游戏中的NPC角色")
                .model(model)
                .stateStore(stateStore)
                .build();
    }
}
