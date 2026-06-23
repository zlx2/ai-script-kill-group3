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
import com.wn.mapper.ScriptRoleMapper;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
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
@Slf4j
@Service
public class NpcDialogueService {

    private final AgentFactory agentFactory;
    private final ScriptRoleMapper scriptRoleMapper;

    public NpcDialogueService(AgentFactory agentFactory, ScriptRoleMapper scriptRoleMapper) {
        this.agentFactory = agentFactory;
        this.scriptRoleMapper = scriptRoleMapper;
    }

    public Mono<String> chat(Long roomId, Long npcRoleId, Long userId, String message) {
        ScriptRole role = scriptRoleMapper.selectById(npcRoleId);
        if (role == null) {
            return Mono.just("角色不存在");
        }

        NpcAgent agent = agentFactory.createNpcAgent(role.getRoleName(), role.getCharacterStory());

        ContentBlock block = TextBlock.builder().text(message).build();
        Msg inputMsg = Msg.builder().content(block).build();

        return agent.call(inputMsg)
                .map(result -> {
                    List<ContentBlock> blocks = result.getContent();
                    return blocks.stream()
                            .filter(b -> b instanceof TextBlock)
                            .map(b -> ((TextBlock) b).getText())
                            .collect(Collectors.joining("\n"));
                });
    }

    public Flux<String> chatStream(Long roomId, Long npcRoleId, Long userId, String message) {
        ScriptRole role = scriptRoleMapper.selectById(npcRoleId);
        if (role == null) {
            return Flux.just("角色不存在");
        }

        NpcAgent agent = agentFactory.createNpcAgent(role.getRoleName(), role.getCharacterStory());

        ContentBlock block = TextBlock.builder().text(message).build();
        Msg inputMsg = Msg.builder().content(block).build();

        return agent.streamEvents(inputMsg)
                .map(event -> {
                    // 根据 AgentEvent 的实际方法提取文本
                    // 你可以按住 Ctrl 点 AgentEvent 看看有什么方法
                    return event.toString();
                });
    }
}
