package com.wn.controller;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:35
 * @Component:
 **/

import jakarta.validation.constraints.NotBlank;
import com.wn.entity.R;
import com.wn.service.NpcDialogueService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * NPC对话接口
 */
@RestController
@RequestMapping("/api/ai/npc")
@Validated
public class NpcDialogueController {

    private final NpcDialogueService npcDialogueService;

    public NpcDialogueController(NpcDialogueService npcDialogueService) {
        this.npcDialogueService = npcDialogueService;
    }

    /**
     * 普通对话（一次性返回）
     */
    @PostMapping("/chat")
    public Mono<R> chat(
            @RequestParam Long roomId,
            @RequestParam Long npcRoleId,
            @RequestParam(required = false) Long userId,
            @RequestParam @NotBlank(message = "消息不能为空") String message
    ) {
        // 兜底：userId为空则赋值游客0
        Long realUserId = userId == null ? 0L : userId;
        return npcDialogueService.chat(roomId, npcRoleId, realUserId, message)
                .map(R::new);
    }

    /**
     * 流式对话（SSE，打字机效果）
     *
     * 【AgentScope知识点32：SSE流式输出】
     * 用text/event-stream类型返回，前端可以用EventSource接收，
     * 实现打字机效果，体验更好。
     *
     * 这是AgentScope流式能力的典型应用场景。
     */
    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam Long roomId,
                                   @RequestParam Long npcRoleId,
                                   @RequestParam Long userId,
                                   @RequestParam String message) {
        return npcDialogueService.chatStream(roomId, npcRoleId, userId, message);
    }
}
