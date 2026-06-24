package com.wn.service.script;

import reactor.core.publisher.Flux;

/**
 * @Author: 杜江
 * @Description: NPC对话服务接口
 * @DateTime: 2026/6/24 17:37
 * @Component:
 **/
public interface NpcDialogueService {
    Flux<String> chatStream(Long roomId, Long npcRoleId, Long userId, String message);
}
