package com.wn.controller.script;

import com.wn.service.script.NpcDialogueService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/24 17:37
 * @Component:
 **/
@RestController
@RequestMapping("/api/room/npc")
public class NpcDialogueController {

    @Resource
    private NpcDialogueService npcDialogueService;

    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestParam Long roomId,
            @RequestParam Long npcRoleId,
            @RequestParam Long userId,
            @RequestParam String message) {

        return npcDialogueService.chatStream(roomId, npcRoleId, userId, message);
    }
}
