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

    private final ScriptMasterAgent scriptMasterAgent;
    private final PlotDesignAgent plotDesignAgent;
    private final RoleDesignAgent roleDesignAgent;
    private final NpcAgent npcAgent;

    public AgentFactory(ScriptMasterAgent scriptMasterAgent,
                        PlotDesignAgent plotDesignAgent,
                        RoleDesignAgent roleDesignAgent,
                        NpcAgent npcAgent) {
        this.scriptMasterAgent = scriptMasterAgent;
        this.plotDesignAgent = plotDesignAgent;
        this.roleDesignAgent = roleDesignAgent;
        this.npcAgent = npcAgent;
    }

    public ScriptMasterAgent createScriptMasterAgent() {
        return scriptMasterAgent;
    }

    public PlotDesignAgent createPlotDesignAgent() {
        return plotDesignAgent;
    }

    public RoleDesignAgent createRoleDesignAgent() {
        return roleDesignAgent;
    }

    public NpcAgent createNpcAgent(String roleName, String characterInfo) {
        npcAgent.setNpcInfo(roleName, characterInfo, characterInfo);
        return npcAgent;
    }
}
