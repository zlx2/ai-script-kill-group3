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
                .sysPrompt("你是专业剧本杀角色设计师。请根据剧本大纲设计角色信息。\n\n" +
                        "要求：\n" +
                        "1. 仅输出标准 JSON 字符串，不要添加任何标题、注释、markdown 代码块、解释文字\n" +
                        "2. 直接输出 JSON，不要附带多余描述\n" +
                        "3. JSON 格式为数组，每个元素包含：roleName(字符串)、gender(字符串，男/女)、age(整数)、characterStory(字符串)、secretInfo(字符串)\n" +
                        "4. 确保 gender、age、secretInfo 字段不为空")
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
