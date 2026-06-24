package com.wn.test.t;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.extensions.redis.state.RedisAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.tools.ToolsConfig;
import io.lettuce.core.RedisClient;


import java.util.List;
import java.util.Scanner;

/**
 * Redis 持久化版的剧本杀NPC对话 Demo测试
 */
public class RedisDemo {

    public static void main(String[] args) {
        // 1. 读取配置
        String apiKey = getEnv("DEEPSEEK_API_KEY");
        String baseUrl = "https://api.deepseek.com";
        String modelName = "deepseek-chat";  // 建议用正确模型名

        // 2. 大模型
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .generateOptions(GenerateOptions.builder()
                        .temperature(0.7).maxTokens(4096).build())
                .build();

        // 3. 替换为 RedisAgentStateStore
        RedisClient redisClient = RedisClient.create("redis://woniuxy@192.168.120.12:6379");
        RedisAgentStateStore stateStore = RedisAgentStateStore.builder()
                .lettuceClient(redisClient)
                .build();

        ToolsConfig toolsConfig = new  ToolsConfig();

        // 4. 构建 HarnessAgent
        HarnessAgent agent = HarnessAgent.builder()
                .toolsConfig(toolsConfig)
                .name("old-butler")
                .sysPrompt("""
                        你是剧本杀《庄园谋杀案》的NPC【管家老张】。
                        你在庄园工作三十年，知道所有秘密但绝不明说。
                        你说话客气恭敬，话里藏话，用暗示引导玩家。
                        """)
                .model(model)
                .stateStore(stateStore)  // ← 注入 Redis 版
                .build();

        System.out.println("=".repeat(50));
        System.out.println("  【剧本杀NPC对话 Demo - Redis 持久化版】");
        System.out.println("  输入格式： userId:sessionId:消息");
        System.out.println("  例如： 1001:room_1:你好");
        System.out.println("=".repeat(50));

        // 5. 交互循环
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n> ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("quit")) break;
                // 解析输入，提取 userId、sessionId、消息以：为分隔，最多分成三份就比如说 userId:sessionId:消息
                String[] parts = input.split(":", 3);
                if (parts.length < 3) {
                    System.out.println("格式错误！正确格式： userId:sessionId:消息");
                    continue;
                }

                RuntimeContext ctx = RuntimeContext.builder()
                        .userId(parts[0].trim())
                        .sessionId(parts[1].trim())
                        .build();

                Msg msg = Msg.builder()
                        .content(TextBlock.builder().text(parts[2].trim()).build())
                        .build();

                String reply = agent.call(msg, ctx)
                        .map(result -> {
                            List<ContentBlock> blocks = result.getContent();
                            if (blocks == null || blocks.isEmpty()) return "";
                            return blocks.stream()
                                    .filter(b -> b instanceof TextBlock)
                                    .map(b -> ((TextBlock) b).getText())
                                    .reduce((a, b) -> a + "\n" + b)
                                    .orElse("");
                        })
                        .block();

                System.out.println("  [管家老张]: " + reply);
            }
        }
    }

    private static String getEnv(String apiKey) {
        String val = System.getenv(apiKey);
        if (val == null || val.isBlank())
            throw new RuntimeException("请设置环境变量 " + apiKey);
        return val;
    }
}

