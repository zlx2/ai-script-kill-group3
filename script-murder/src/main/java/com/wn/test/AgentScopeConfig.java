/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/23 12:04
 * @Component:
 **/
package com.wn.test;

import com.wn.test.ScriptKillTools;
import io.agentscope.core.ReActAgent;
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
    public ReActAgent dmAgent(Toolkit scriptToolkit) {
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
        return ReActAgent.builder()
                .name("script_dm_host")
                // 官方推荐字符串modelId，自动读取配置里的dashscope.api-key
                .model("dashscope:qwen-turbo")
                .sysPrompt(dmSystemPrompt)
                .toolkit(scriptToolkit)
                .maxIters(20)
                .build();
    }

    /**
     * 动态创建玩家Agent
     * @param name 玩家昵称
     * @param identity 角色身份
     * @param secret 个人隐藏秘密
     * @return 独立玩家ReActAgent
     */
    public ReActAgent createPlayerAgent(String name, String identity, String secret) {
        String playerPrompt = String.format("""
                你是剧本杀玩家【%s】，角色身份：%s，你的隐藏秘密：%s
                行为规则：
                1. 不要主动暴露自己的秘密；
                2. 如果你是凶手，需要合理嫁祸其他嫌疑人；
                3. 需要搜证时调用search_clue工具，投票阶段调用vote_suspect；
                4. 发言简短自然，贴合自身角色人设。
                """, name, identity, secret);
        return ReActAgent.builder()
                .name("player_" + name)
                .model("dashscope:qwen-turbo")
                .sysPrompt(playerPrompt)
                .maxIters(10)
                .build();
    }
}
