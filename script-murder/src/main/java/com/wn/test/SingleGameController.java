/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/23 14:25
 * @Component:
 **/
package com.wn.test;

import com.wn.entity.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/game")
public class SingleGameController {

    @Resource
    private ScriptKillGameService gameService;

    // 1. 创建房间
    @PostMapping("/start")
    public R startRoom(@RequestParam String roomId) {
        return new R(gameService.startGame(roomId));
    }

    // 2. 玩家自选角色
    @PostMapping("/role/select")
    public R selectRole(@RequestParam String roomId,
                             @RequestParam String userId,
                             @RequestParam String roleName) {
        return new R(gameService.selectRole(roomId, userId, roleName));
    }

    // 3. 角色自我介绍
    @PostMapping("/intro")
    public R selfIntro(@RequestParam String roomId,
                            @RequestParam String userId,
                            @RequestParam String roleName) {
        return new R(gameService.selfIntro(roomId, userId, roleName));
    }

    // 4. 玩家搜证（唯一获取线索入口）
    @PostMapping("/search")
    public R search(@RequestParam String roomId,
                         @RequestParam String userId,
                         @RequestParam String roleName,
                         @RequestParam String scene) {
        return new R(gameService.searchEvidence(roomId, userId, roleName, scene));
    }

    // 5. 公聊发言（全局所有人可见）
    @PostMapping("/chat/public")
    public R publicChat(@RequestParam String roomId,
                             @RequestParam String userId,
                             @RequestParam String roleName,
                             @RequestBody String content) {
        return new R(gameService.publicChat(roomId, userId, roleName, content));
    }

    // 6. 私聊发言（仅发送方、接收方私有记录可见）
    @PostMapping("/chat/private")
    public R privateChat(@RequestParam String roomId,
                              @RequestParam String userId,
                              @RequestParam String sender,
                              @RequestParam String receiver,
                              @RequestBody String content) {
        return new R(gameService.privateChat(roomId, userId, sender, receiver, content));
    }

    // 7. 玩家投票
    @PostMapping("/vote")
    public R vote(@RequestParam String roomId,
                       @RequestParam String userId,
                       @RequestParam String voter,
                       @RequestParam String targetRole) {
        return new R(gameService.voteSuspect(roomId, userId, voter, targetRole));
    }

    // 8. DM切换下一游戏阶段
    @PostMapping("/nextStage")
    public R nextStage(@RequestParam String roomId) {
        return new R(gameService.nextGameStage(roomId));
    }

    // ========== 查询接口 ==========
    // 获取全局公聊全部历史（所有玩家可看）
    @GetMapping("/history/global")
    public R getGlobalHistory(@RequestParam String roomId) {
        return new R(gameService.getGlobalHistory(roomId));
    }

    // 获取当前角色私有对话+私有线索记录（仅本人）
    @GetMapping("/history/private")
    public R getPrivateHistory(@RequestParam String roomId,
                                    @RequestParam String userId,
                                    @RequestParam String roleName) {
        return new R(gameService.getRolePrivateHistory(roomId, userId, roleName));
    }

    // 查询当前角色全部私有线索（前端线索面板）
    @GetMapping("/clue/my")
    public R getMyClues(@RequestParam String roomId,
                                   @RequestParam String userId,
                                   @RequestParam String roleName) {
        return new R(gameService.getRolePrivateClues(roomId, userId, roleName));
    }

    // 查询房间已选择角色列表
    @GetMapping("/room/roles")
    public R getSelectedRoles(@RequestParam String roomId) {
        return new R(gameService.getRoomSelectedRoles(roomId));
    }

    // 查询当前房间游戏阶段
    @GetMapping("/stage")
    public R getStage(@RequestParam String roomId) {
        return new R(gameService.getCurrentStage(roomId));
    }

    // 销毁房间，释放缓存
    @DeleteMapping("/destroy")
    public R destroyRoom(@RequestParam String roomId) {
        gameService.destroyRoom(roomId);
        return new R("房间[" + roomId + "]缓存已全部清理完成");
    }
}