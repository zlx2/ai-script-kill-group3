package com.wn.controller;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:35
 * @Component:
 **/

import com.wn.entity.R;
import com.wn.service.NpcDialogueService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * NPC对话接口
 */
@RestController
@RequestMapping("/api/ai/npc")
public class NpcDialogueController {

    private final NpcDialogueService npcDialogueService;

    public NpcDialogueController(NpcDialogueService npcDialogueService) {
        this.npcDialogueService = npcDialogueService;
    }

    /**
     * 普通对话（一次性返回）
     */
    @PostMapping("/chat")
    public R chat(@RequestParam Long roomId,
                  @RequestParam Long npcRoleId,
                  @RequestParam Long userId,
                  @RequestParam String message) {
        String reply = npcDialogueService.chat(roomId, npcRoleId, userId, message);
        return new R(reply);
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
