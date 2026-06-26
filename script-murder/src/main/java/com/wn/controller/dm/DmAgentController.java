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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dm/agent")
@RequiredArgsConstructor
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

    @PostMapping("/auto-run")
    public R autoRun(@RequestParam String roomId) {
        String result = dmAgentService.autoRun(roomId);
        return new R(result);
    }

    @PostMapping("/start-game")
    public R startGame(@RequestParam String roomId, @RequestParam Long scriptId) {
        String result = dmAgentService.startGame(roomId, scriptId);
        return new R("AI DM已接管游戏\n" + result);
    }
}
