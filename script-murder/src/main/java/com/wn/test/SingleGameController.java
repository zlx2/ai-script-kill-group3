/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/23 14:25
 * @Component:
 **/
package com.wn.test;

import com.wn.test.ScriptKillGameService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/game")
public class SingleGameController {

    @Resource
    private ScriptKillGameService gameService;

    /**
     * 开启单人剧本杀房间
     */
    @GetMapping("/start")
    public String startGame(@RequestParam String roomId, @RequestParam String humanRole) {
        return gameService.startGame(roomId, humanRole);
    }

    /**
     * 真人角色自我介绍
     */
    @GetMapping("/intro/self")
    public String humanSelfIntro(@RequestParam String roomId) {
        String humanRole = gameService.roomHumanRole.get(roomId);
        return gameService.roleSelfIntro(roomId, humanRole);
    }

    /**
     * 自动执行剩余两个AI角色自我介绍
     */
    @GetMapping("/intro/ai")
    public String aiIntro(@RequestParam String roomId) {
        return gameService.aiAllSelfIntro(roomId);
    }

    /**
     * 真人发言/搜证/投票
     */
    @GetMapping("/chat")
    public String humanTalk(@RequestParam String roomId, @RequestParam String msg) {
        return gameService.humanPlayerChat(roomId, msg);
    }

    /**
     * DM推进下一阶段
     */
    @GetMapping("/next")
    public String nextStage(@RequestParam String roomId) {
        return gameService.dmNextStage(roomId);
    }

    /**
     * AI角色一轮自由发言
     */
    @GetMapping("/ai/talk")
    public String aiAutoTalk(@RequestParam String roomId) {
        return gameService.aiOtherRoleTalk(roomId);
    }

    /**
     * 查看全部对话记录
     */
    @GetMapping("/history")
    public String getHistory(@RequestParam String roomId) {
        return gameService.getRoomHistory(roomId);
    }
}
