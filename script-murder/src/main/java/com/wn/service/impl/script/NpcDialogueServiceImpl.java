package com.wn.service.impl.script;

import com.alibaba.fastjson2.JSON;
import com.wn.agent.NpcAgent;
import com.wn.entity.chat.ChatMessagePO;
import com.wn.entity.script.ScriptRolePO;
import com.wn.mapper.script.ScriptRoleMapper;
import com.wn.service.script.NpcDialogueService;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: 杜江
 * @Description: NPC对话服务实现类
 * @DateTime: 2026/6/24 17:38
 * @Component:
 **/
@Slf4j
@Service
public class NpcDialogueServiceImpl implements NpcDialogueService {

    @Resource
    private HarnessAgent npcDialogueAgent;

    @Resource
    private ScriptRoleMapper scriptRoleMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CHAT_HISTORY_KEY = "chat:history:%d:%d";
    private static final int MAX_HISTORY_SIZE = 20;

    @Override
    public Flux<String> chatStream(Long roomId, Long npcRoleId, Long userId, String message) {
        ScriptRolePO role = scriptRoleMapper.findById(npcRoleId).orElse(null);
        if (role == null) {
            return Flux.just("角色不存在");
        }

        String historyKey = String.format(CHAT_HISTORY_KEY, roomId, npcRoleId);
        List<ChatMessagePO> history = getChatHistory(historyKey);

        String context = buildContext(role, history);
        String prompt = context + "\n\n用户最新消息：" + message;

        Msg msg = Msg.builder()
                .content(TextBlock.builder().text(prompt).build())
                .build();

        return npcDialogueAgent.call(msg)
                .flatMapMany(result -> {
                    String fullReply = result.getContent().stream()
                            .filter(b -> b instanceof TextBlock)
                            .map(b -> ((TextBlock) b).getText())
                            .collect(Collectors.joining("\n"));

                    saveChatHistory(historyKey, userId, npcRoleId, role.getRoleName(), message, "user");
                    saveChatHistory(historyKey, userId, npcRoleId, role.getRoleName(), fullReply, "npc");
                    log.info("对话完成，roomId={}, npcRoleId={}, replyLength={}",
                            roomId, npcRoleId, fullReply.length());

                    return Flux.create(emitter -> {
                        int chunkSize = 5;
                        for (int i = 0; i < fullReply.length(); i += chunkSize) {
                            int end = Math.min(i + chunkSize, fullReply.length());
                            emitter.next(fullReply.substring(i, end));
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                emitter.error(e);
                                return;
                            }
                        }
                        emitter.complete();
                    });
                });
    }

    private String buildContext(ScriptRolePO role, List<ChatMessagePO> history) {
        StringBuilder context = new StringBuilder();
        context.append("你是剧本杀游戏中的NPC角色【").append(role.getRoleName()).append("】。\n");
        context.append("性别：").append(role.getGender() != null ? role.getGender() : "未知").append("\n");
        context.append("年龄：").append(role.getAge() != null ? role.getAge() : "未知").append("\n");
        context.append("角色故事：").append(role.getCharacterStory() != null ? role.getCharacterStory() : "").append("\n");
        context.append("秘密信息：").append(role.getSecretInfo() != null ? role.getSecretInfo() : "").append("\n");

        if (!history.isEmpty()) {
            context.append("\n历史对话：\n");
            for (ChatMessagePO msg : history) {
                context.append(msg.getRole().equals("user") ? "玩家" : msg.getNpcRoleName())
                        .append(": ")
                        .append(msg.getContent())
                        .append("\n");
            }
        }

        return context.toString();
    }

    @SuppressWarnings("unchecked")
    private List<ChatMessagePO> getChatHistory(String key) {
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj != null) {
            try {
                return JSON.parseArray(obj.toString(), ChatMessagePO.class);
            } catch (Exception e) {
                log.warn("解析聊天历史失败", e);
            }
        }
        return new ArrayList<>();
    }

    private void saveChatHistory(String key, Long userId, Long npcRoleId, String roleName, String content, String role) {
        List<ChatMessagePO> history = getChatHistory(key);

        ChatMessagePO message = ChatMessagePO.builder()
                .roomId(Long.parseLong(key.split(":")[2]))
                .userId(userId)
                .npcRoleId(npcRoleId)
                .npcRoleName(roleName)
                .content(content)
                .role(role)
                .createTime(LocalDateTime.now())
                .build();

        history.add(message);

        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
        }

        redisTemplate.opsForValue().set(key, JSON.toJSONString(history), 24, TimeUnit.HOURS);
    }
}
