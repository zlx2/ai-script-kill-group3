package com.wn.test.t;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import io.agentscope.extensions.redis.state.RedisAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 最简单的 RC3 Demo — 一个文件，复制粘贴直接运行 main()
 * 控制台和 NPC 管家聊天，自动记忆上下文。
 * 目前是存储在内存中
 */
public class SimpleDemo {

    public static void main(String[] args) {
        String apiKey = getEnv("DEEPSEEK_API_KEY");  // ← 改为从 DEEPSEEK_API_KEY 读取
        String baseUrl = "https://api.deepseek.com";
        String modelName = "deepseek-v4-flash";

        // 1. 大模型
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .generateOptions(GenerateOptions.builder()
                        .temperature(0.7).maxTokens(4096).build())
                .build();

        // 2. 状态存储
        AgentStateStore stateStore = new SimpleStateStore();

        // 3. 构建 HarnessAgent（注入 StateStore）
        HarnessAgent agent = HarnessAgent.builder()
                .name("old-butler")
                .sysPrompt("""
                        你是剧本杀《庄园谋杀案》的NPC【管家老张】。
                        你在庄园工作三十年，知道所有秘密但绝不明说。
                        你说话客气恭敬，话里藏话，用暗示引导玩家。
                        """)
                .model(model)
                .stateStore(stateStore)
                .build();

        System.out.println("=".repeat(50));
        System.out.println("  【剧本杀NPC对话 Demo】");
        System.out.println("  输入格式： userId:sessionId:消息");
        System.out.println("  例如： 1001:room_1:你好");
        System.out.println("         1001:room_1:你叫什么名字  （同一用户，对话连续）");
        System.out.println("         1002:room_1:你好          （不同用户，历史隔离）");
        System.out.println("=".repeat(50));

        //用户输出，输入格式： userId:sessionId:消息，强制要求，不然存储的key有问题
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n> ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("quit")) break;

                String[] parts = input.split(":", 3);
                if (parts.length < 3) {
                    System.out.println("格式错误！正确格式： userId:sessionId:消息");
                    continue;
                }

                // 4. 构建 RuntimeContext（隔离钥匙）
                RuntimeContext ctx = RuntimeContext.builder()
                        .userId(parts[0].trim())
                        .sessionId(parts[1].trim())
                        .build();

                Msg msg = Msg.builder()
                        .content(TextBlock.builder().text(parts[2].trim()).build())
                        .build();

                // 5. 调用 HarnessAgent（自动读写 StateStore）
                String reply = agent.call(msg, ctx)
                        .map(result -> {
                            List<ContentBlock> blocks = result.getContent();
                            if (blocks == null || blocks.isEmpty()) return "";
                            return blocks.stream()
                                    .filter(b -> b instanceof TextBlock)
                                    .map(b -> ((TextBlock) b).getText())
                                    .reduce((a, b) -> a + "\n" + b).orElse("");
                        }).block();

                System.out.println("  [管家老张]: " + reply);
            }
        }
    }

    private static String getEnv(String key) {
        String val = System.getenv(key);
        if (val == null || val.isBlank())
            throw new RuntimeException("请设置环境变量 " + key);
        return val;
    }

    private static String getEnv(String key, String defaultVal) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultVal;
    }

    // 重写默认的  StateStore 太复杂，直接别看了，难以自定义实现
    static class SimpleStateStore implements AgentStateStore {
        private final Map<String, Map<String, Map<String, State>>> store = new ConcurrentHashMap<>();

        @Override public void save(String userId, String sessionId, String key, State value) {
            if (isEmpty(userId) || isEmpty(sessionId) || isEmpty(key)) return;
            store.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(key, value);
        }
        @Override public void save(String userId, String sessionId, String key, List<? extends State> values) {}
        @Override public <T extends State> List<T> getList(String userId, String sessionId, String key, Class<T> itemType) {
            return Collections.emptyList();
        }
        @Override public <T extends State> Optional<T> get(String userId, String sessionId, String key, Class<T> type) {
            if (isEmpty(userId)) return Optional.empty();
            var userStore = store.get(userId);
            if (userStore == null) return Optional.empty();
            var sessionStore = userStore.get(sessionId);
            if (sessionStore == null) return Optional.empty();
            State value = sessionStore.get(key);
            if (!type.isInstance(value)) return Optional.empty();
            return Optional.of((T) value);
        }
        @Override public boolean exists(String userId, String sessionId) {
            if (isEmpty(userId)) return false;
            var userStore = store.get(userId);
            return userStore != null && userStore.containsKey(sessionId);
        }
        @Override public void delete(String userId, String sessionId) {
            if (isEmpty(userId)) return;
            var userStore = store.get(userId);
            if (userStore != null) userStore.remove(sessionId);
        }
        @Override public void delete(String userId, String sessionId, String key) {
            if (isEmpty(userId)) return;
            var userStore = store.get(userId);
            if (userStore != null) {
                var sessionStore = userStore.get(sessionId);
                if (sessionStore != null) sessionStore.remove(key);
            }
        }
        @Override public Set<String> listSessionIds(String userId) {
            if (isEmpty(userId)) return Collections.emptySet();
            var userStore = store.get(userId);
            return userStore != null ? new HashSet<>(userStore.keySet()) : Collections.emptySet();
        }
        @Override public void close() { store.clear(); }
        private boolean isEmpty(String s) { return s == null || s.isBlank(); }
    }
}
