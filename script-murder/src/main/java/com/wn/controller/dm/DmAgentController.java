/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/25 15:01
 * @Component:
 **/
package com.wn.controller.dm;

import com.wn.entity.R;
import com.wn.service.dm.DmAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/dm/agent")
@RequiredArgsConstructor
@Slf4j
public class DmAgentController {

    private final DmAgentService dmAgentService;

    @PostMapping("/init")
    public R initAgent(@RequestParam String roomId, @RequestParam Long scriptId) {
        dmAgentService.initAgent(roomId, scriptId);
        return new R(200, "AI DM初始化完成");
    }

    @PostMapping("/analyze")
    public R analyzeGame(@RequestParam String roomId, @RequestBody String gameContext) {
        String decision = dmAgentService.analyzeGame(roomId, gameContext);
        return new R(decision);
    }

    @PostMapping("/execute")
    public R executeDecision(@RequestParam String roomId, @RequestBody String decisionJson) {
        String result = dmAgentService.executeDecision(roomId, decisionJson);
        return new R(result);
    }

    @GetMapping(value = "/auto-run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> autoRun(@RequestParam String roomId) {
        return dmAgentService.autoRun(roomId)
                .map(data -> "data: " + data + "\n\n")
                .doOnSubscribe(subscription -> log.info("SSE连接建立, roomId={}", roomId))
                .doOnComplete(() -> log.info("SSE连接关闭, roomId={}", roomId))
                .doOnError(e -> log.error("SSE连接异常, roomId={}", roomId, e));
    }

    @GetMapping(value = "/start-game", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> startGame(@RequestParam String roomId, @RequestParam Long scriptId) {
        return dmAgentService.startGame(roomId, scriptId);
    }
}
