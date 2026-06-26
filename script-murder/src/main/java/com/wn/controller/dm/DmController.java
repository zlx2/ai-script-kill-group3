/**
 * @Author: 杜江
 * @Description:DM手动控制器
 * @DateTime: 2026/6/25 15:10
 * @Component:
 **/
package com.wn.controller.dm;

import com.wn.entity.R;
import com.wn.service.dm.*;
import com.wn.service.script.GameQuestionService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dm")
@RequiredArgsConstructor
public class DmController {

    private final DmScriptService scriptService;
    private final DmRoomService roomService;
    private final DmActService actService;
    private final DmVoteService voteService;
    private final DmPlayerService playerService;
    @Resource
    private GameQuestionService questionService;

    // ==================== DM手册管理 ====================

    @GetMapping("/script/stages")
    public R getStages(@RequestParam Long scriptId) {
        return new R(scriptService.getScriptStages(scriptId));
    }

    @GetMapping("/script/hints")
    public R getHints(@RequestParam Long scriptId) {
        return new R(scriptService.getScriptHints(scriptId));
    }

    @GetMapping("/script/hints/level")
    public R getHintsByLevel(@RequestParam Long scriptId, @RequestParam Byte level) {
        return new R(scriptService.getScriptHintsByLevel(scriptId, level));
    }

    @GetMapping("/script/review")
    public R getReview(@RequestParam Long scriptId) {
        return new R(scriptService.getScriptReview(scriptId));
    }

    // ==================== 房间状态管理 ====================
    @PostMapping("/room/welcome")
    public R sendWelcomeMessage(@RequestParam String roomId, @RequestParam Long scriptId) {
        roomService.sendWelcomeMessage(roomId, scriptId);
        return new R(200, "欢迎消息已发送");
    }

    @GetMapping("/room/state")
    public R getRoomState(@RequestParam String roomId) {
        return new R(roomService.getRoomState(roomId));
    }

    @PutMapping("/room/settings")
    public R updateSettings(@RequestParam String roomId, @RequestBody(required = false) Map<String, Object> settings) {
        roomService.updateRoomSettings(roomId, settings);
        return new R(200, "更新成功");
    }

    @PostMapping("/room/private-chat/enable")
    public R enablePrivateChat(@RequestParam String roomId) {
        roomService.enablePrivateChat(roomId);
        return new R(200, "私聊已开启");
    }

    @PostMapping("/room/private-chat/disable")
    public R disablePrivateChat(@RequestParam String roomId) {
        roomService.disablePrivateChat(roomId);
        return new R(200, "私聊已关闭");
    }

    @PostMapping("/room/ai-talk/enable")
    public R enableAiTalk(@RequestParam String roomId) {
        roomService.enableAiTalk(roomId);
        return new R(200, "AI对话已开启");
    }

    @PostMapping("/room/ai-talk/disable")
    public R disableAiTalk(@RequestParam String roomId) {
        roomService.disableAiTalk(roomId);
        return new R(200, "AI对话已关闭");
    }

    @PutMapping("/room/search-round")
    public R setSearchRound(@RequestParam String roomId, @RequestParam Integer count) {
        roomService.setSearchRoundCount(roomId, count);
        return new R(200, "搜证轮数已设置");
    }

    @PutMapping("/room/chat-duration")
    public R setChatDuration(@RequestParam String roomId, @RequestParam Integer minutes) {
        roomService.setChatDuration(roomId, minutes);
        return new R(200, "聊天时长已设置");
    }

    // ==================== 阶段推进 ====================

    @PostMapping("/act/advance")
    public R advanceAct(@RequestParam String roomId) {
        return new R(actService.advanceAct(roomId));
    }

    @GetMapping("/act/current")
    public R getCurrentAct(@RequestParam String roomId) {
        return new R(actService.getCurrentAct(roomId));
    }

    @PostMapping("/act/rollback")
    public R rollbackAct(@RequestParam String roomId) {
        actService.rollbackAct(roomId);
        return new R(200, "已回滚");
    }

    // ==================== 投票系统 ====================

    @PostMapping("/vote/start")
    public R startVoting(@RequestParam String roomId) {
        voteService.startVoting(roomId);
        return new R(200, "投票已开始");
    }

    @PostMapping("/vote/end")
    public R endVoting(@RequestParam String roomId) {
        voteService.endVoting(roomId);
        return new R(200, "投票已结束");
    }

    @GetMapping("/vote/results")
    public R getVoteResults(@RequestParam String roomId) {
        return new R(voteService.getVoteResults(roomId));
    }

    @GetMapping("/vote/count")
    public R getVoteCount(@RequestParam String roomId) {
        return new R(voteService.getVoteCount(roomId));
    }

    // ==================== 玩家管理 ====================

    @PostMapping("/player/assign-role")
    public R assignRole(
            @RequestParam String roomId,
            @RequestParam Long playerId,
            @RequestParam Long roleId,
            @RequestParam Long scriptId) {
        playerService.assignRole(roomId, playerId, roleId, scriptId);
        return new R(200, "角色已分配");
    }

    @PostMapping("/player/mute")
    public R mutePlayer(@RequestParam String roomId, @RequestParam Long playerId) {
        playerService.mutePlayer(roomId, playerId);
        return new R(200, "已静音");
    }

    @PostMapping("/player/unmute")
    public R unmutePlayer(@RequestParam String roomId, @RequestParam Long playerId) {
        playerService.unmutePlayer(roomId, playerId);
        return new R(200, "已解除静音");
    }

    @GetMapping("/player/task")
    public R getPlayerTask(@RequestParam String roomId, @RequestParam Long playerId) {
        return new R(playerService.getPlayerTask(roomId, playerId));
    }

    @PutMapping("/player/task")
    public R updatePlayerTask(@RequestParam String roomId, @RequestParam Long playerId, @RequestBody(required = false) String taskProgress) {
        playerService.updatePlayerTask(roomId, playerId, taskProgress);
        return new R(200, "任务进度已更新");
    }

    // ==================== 题目管理 ====================

    @GetMapping("/question/list/{scriptId}")
    public R listQuestionByScript(@PathVariable Long scriptId) {
        return questionService.listAllQuestionByScript(scriptId);
    }

    @GetMapping("/question/detail/{id}")
    public R getQuestionDetail(@PathVariable Long id) {
        return questionService.getQuestionDetail(id);
    }

    @GetMapping("/question/role/{scriptId}/{roleId}")
    public R getQuestionByRole(@PathVariable Long scriptId, @PathVariable Long roleId) {
        return questionService.getQuestionByRole(scriptId, roleId);
    }
}