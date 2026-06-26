/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/23 12:04
 * @Component:
 **/
package com.wn.test;

import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.core.tool.Toolkit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AgentScopeConfig {

    // 全局工具集
    @Bean
    public Toolkit scriptToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new ScriptKillTools());
        return toolkit;
    }

    // DM主持人Agent，采用官方推荐字符串model id写法
    @Bean
    public HarnessAgent testDmAgent(Toolkit scriptToolkit) {
        String dmSystemPrompt = """
                你是专业剧本杀DM，本局剧本设定：
                死者：庄园主张老爷，死于密室书房，真正凶手是女仆小兰。
                本场玩家角色：女仆小兰（真凶）、商人老李、医生老王。
                游戏固定四阶段流程：1.开局介绍剧本 2.自由搜证 3.公聊推理 4.投票+复盘。
                工具调用规则：
                1. 玩家需要搜证时，自动调用 search_clue 工具；
                2. 投票阶段引导玩家调用 vote_suspect 工具；
                3. 全程禁止提前泄露凶手身份；
                4. 投票结束后汇总票数，完整复盘作案动机与密室手法。
                说话简短口语化，分阶段引导游戏推进。
                """;
        return HarnessAgent.builder()
                .name("script_dm_host")
                // 官方推荐字符串modelId，自动读取配置里的dashscope.api-key
                .model("dashscope:qwen-turbo")
                .sysPrompt(dmSystemPrompt)
                .toolkit(scriptToolkit)
                .maxIters(20)
                .build();
    }

}
