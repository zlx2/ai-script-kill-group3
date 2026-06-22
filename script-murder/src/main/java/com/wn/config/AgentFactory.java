package com.wn.config;

import com.wn.ai.agent.NpcAgent;
import com.wn.ai.agent.PlotDesignAgent;
import com.wn.ai.agent.RoleDesignAgent;
import com.wn.ai.agent.ScriptMasterAgent;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.state.AgentStateStore;
import org.springframework.stereotype.Component;

@Component
public class AgentFactory {

    private final OpenAIChatModel chatModel;
    private final AgentStateStore agentStateStore;

    public AgentFactory(OpenAIChatModel chatModel, AgentStateStore agentStateStore) {
        this.chatModel = chatModel;
        this.agentStateStore = agentStateStore;
    }

    /**
     * 创建剧本生成总控Agent
     */
    public ScriptMasterAgent createScriptMasterAgent() {
        return ScriptMasterAgent.builder()
                .name("script-master")
                .model(chatModel)
                .stateStore(agentStateStore)
                .build();
    }

    /**
     * 创建剧情策划Agent
     */
    public PlotDesignAgent createPlotDesignAgent() {
        return PlotDesignAgent.builder()
                .name("plot-designer")
                .model(chatModel)
                .stateStore(agentStateStore)
                .build();
    }

    /**
     * 创建角色设计Agent
     */
    public RoleDesignAgent createRoleDesignAgent() {
        return RoleDesignAgent.builder()
                .name("role-designer")
                .model(chatModel)
                .stateStore(agentStateStore)
                .build();
    }

//    /**
//     * 创建线索设计Agent
//     */
//    public ClueDesignAgent createClueDesignAgent() {
//        return ClueDesignAgent.builder()
//                .name("clue-designer")
//                .model(chatModel)
//                .stateStore(agentStateStore)
//                .build();
//    }
//
//    /**
//     * 创建逻辑校验Agent
//     */
//    public LogicCheckAgent createLogicCheckAgent() {
//        return LogicCheckAgent.builder()
//                .name("logic-checker")
//                .model(chatModel)
//                .stateStore(agentStateStore)
//                .build();
//    }

    /**
     * 创建NPC对话Agent（每个NPC一个实例）
     *
     * 【AgentScope知识点12：动态创建Agent】
     * NPC对话这种场景，每个NPC有不同的人设，
     * 所以需要根据角色信息动态创建Agent，设置不同的System Prompt。
     * 就像每个演员有不同的剧本，上场前要给他对戏。
     */
    public NpcAgent createNpcAgent(String roleName, String characterInfo) {
        return NpcAgent.builder()
                .name("npc-" + roleName)
                .model(chatModel)
                .stateStore(agentStateStore)
                .npcName(roleName)
                .npcPersonality(characterInfo)
                .npcBackground(characterInfo)
//                .tools(null)
                .build();
    }
}
