package com.wn.service;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:32
 * @Component:
 **/

import com.wn.ai.agent.NpcAgent;
import com.wn.config.AgentFactory;
import com.wn.entity.ScriptRole;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * NPC对话服务
 *
 * 【AgentScope知识点29：会话复用】
 * 每个NPC在每个房间里只有一个Agent实例，
 * 玩家多次对话都用同一个Agent，这样对话历史是连续的。
 *
 * 我们把创建好的Agent存在缓存里（Redis或本地Map），
 * 下次对话直接取出来用，不用重新创建。
 *
 * 就像你去商店，每次都是同一个店员接待你，
 * 他记得你上次买了什么，不用每次都重新认识。
 */
@Service
public class NpcDialogueService {

    private final AgentFactory agentFactory;
    private final ScriptRoleService scriptRoleService;
    private final RedisTemplate<String, Object> redisTemplate;

    // 本地缓存：roomId:npcRoleId -> NpcAgent
    // 实际项目中建议用Redis存会话状态，本地只存Agent引用
    private final ConcurrentHashMap<String, NpcAgent> npcAgentCache = new ConcurrentHashMap<>();

    public NpcDialogueService(AgentFactory agentFactory,
                              ScriptRoleService scriptRoleService,
                              RedisTemplate<String, Object> redisTemplate) {
        this.agentFactory = agentFactory;
        this.scriptRoleService = scriptRoleService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 和NPC对话（普通模式，一次性返回）
     */
    public String chat(Long roomId, Long npcRoleId, Long userId, String message) {
        // 1. 获取或创建NPC Agent
        NpcAgent npcAgent = getOrCreateNpcAgent(roomId, npcRoleId);

        // 2. 构建消息
        ContentBlock block = TextBlock.builder().text(message).build();
        Msg inputMsg = Msg.builder().content(block).build();

        // 3. 调用Agent
        Msg result = npcAgent.call(inputMsg).block();

        // 4. 返回回复内容
        return result.getContent().stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).getText())
                .collect(Collectors.joining("\n"));
    }

    /**
     * 和NPC对话（流式模式，打字机效果）
     *
     * 【AgentScope知识点30：流式输出事件】
     * streamEvents()返回的是一个事件流，包含多种事件类型：
     * - AgentStartEvent：Agent开始处理
     * - ThoughtEvent：思考内容（ReAct模式）
     * - ToolCallEvent：调用工具
     * - ToolResultEvent：工具返回结果
     * - MessageEvent：最终回复消息
     * - AgentEndEvent：Agent处理结束
     *
     * 前端可以根据不同事件类型做不同的UI展示。
     */
    public Flux<String> chatStream(Long roomId, Long npcRoleId, Long userId, String message) {
        NpcAgent npcAgent = getOrCreateNpcAgent(roomId, npcRoleId);

        ContentBlock block = TextBlock.builder().text(message).build();
        Msg inputMsg = Msg.builder().content(block).build();

        // 返回流式事件，前端用SSE接收
        return npcAgent.streamEvents(inputMsg)
                .map(event -> {
                    // 根据 AgentEvent 的实际方法提取内容
                    // 你可以按住 Ctrl 点 AgentEvent 看看有什么方法
                    return event.toString();
                });
    }

    /**
     * 获取或创建NPC Agent
     */
    private NpcAgent getOrCreateNpcAgent(Long roomId, Long npcRoleId) {
        String cacheKey = roomId + ":" + npcRoleId;

        // 先从缓存取
        NpcAgent agent = npcAgentCache.get(cacheKey);
        if (agent != null) {
            return agent;
        }

        // 缓存没有，创建新的
        ScriptRole role = scriptRoleService.getById(npcRoleId);

        String characterInfo = String.format(
                "姓名：%s\n性别：%s\n年龄：%d岁\n背景故事：%s\n秘密：%s",
                role.getRoleName(),
                role.getGender() == 1 ? "男" : "女",
                role.getAge(),
                role.getCharacterStory(),
                role.getSecretInfo()
        );

        NpcAgent newAgent = agentFactory.createNpcAgent(role.getRoleName(), characterInfo);

        // 【AgentScope知识点31：会话ID】
        // 每个Agent会话有一个唯一的sessionId，
        // 用来从StateStore里恢复状态。
        // 我们用roomId:npcRoleId作为sessionId，
        // 这样同一个房间的同一个NPC，状态是共享的。
        newAgent.setSessionId(cacheKey);

        // 放入缓存
        npcAgentCache.put(cacheKey, newAgent);

        return newAgent;
    }
}
