/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/23 14:25
 * @Component:
 **/
package com.wn.test;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/game")
public class SingleGameController {

    @Resource
    private ScriptKillGameService gameService;

    // 1. 创建房间
    @PostMapping("/start")
    public String startRoom(@RequestParam String roomId) {
        return gameService.startGame(roomId);
    }

    // 2. 玩家自选角色
    @PostMapping("/role/select")
    public String selectRole(@RequestParam String roomId,
                             @RequestParam String userId,
                             @RequestParam String roleName) {
        return gameService.selectRole(roomId, userId, roleName);
    }

    // 3. 角色自我介绍
    @PostMapping("/intro")
    public String selfIntro(@RequestParam String roomId,
                            @RequestParam String userId,
                            @RequestParam String roleName) {
        return gameService.selfIntro(roomId, userId, roleName);
    }

    // 4. 玩家搜证（唯一获取线索入口）
    @PostMapping("/search")
    public String search(@RequestParam String roomId,
                         @RequestParam String userId,
                         @RequestParam String roleName,
                         @RequestParam String scene) {
        return gameService.searchEvidence(roomId, userId, roleName, scene);
    }

    // 5. 公聊发言（全局所有人可见）
    @PostMapping("/chat/public")
    public String publicChat(@RequestParam String roomId,
                             @RequestParam String userId,
                             @RequestParam String roleName,
                             @RequestBody String content) {
        return gameService.publicChat(roomId, userId, roleName, content);
    }

    // 6. 私聊发言（仅发送方、接收方私有记录可见）
    @PostMapping("/chat/private")
    public String privateChat(@RequestParam String roomId,
                              @RequestParam String userId,
                              @RequestParam String sender,
                              @RequestParam String receiver,
                              @RequestBody String content) {
        return gameService.privateChat(roomId, userId, sender, receiver, content);
    }

    // 7. 玩家投票
    @PostMapping("/vote")
    public String vote(@RequestParam String roomId,
                       @RequestParam String userId,
                       @RequestParam String voter,
                       @RequestParam String targetRole) {
        return gameService.voteSuspect(roomId, userId, voter, targetRole);
    }

    // 8. DM切换下一游戏阶段
    @PostMapping("/nextStage")
    public String nextStage(@RequestParam String roomId) {
        return gameService.nextGameStage(roomId);
    }

    // ========== 查询接口 ==========
    // 获取全局公聊全部历史（所有玩家可看）
    @GetMapping("/history/global")
    public String getGlobalHistory(@RequestParam String roomId) {
        return gameService.getGlobalHistory(roomId);
    }

    // 获取当前角色私有对话+私有线索记录（仅本人）
    @GetMapping("/history/private")
    public String getPrivateHistory(@RequestParam String roomId,
                                    @RequestParam String userId,
                                    @RequestParam String roleName) {
        return gameService.getRolePrivateHistory(roomId, userId, roleName);
    }

    // 查询当前角色全部私有线索（前端线索面板）
    @GetMapping("/clue/my")
    public List<String> getMyClues(@RequestParam String roomId,
                                   @RequestParam String userId,
                                   @RequestParam String roleName) {
        return gameService.getRolePrivateClues(roomId, userId, roleName);
    }

    // 查询房间已选择角色列表
    @GetMapping("/room/roles")
    public Set<String> getSelectedRoles(@RequestParam String roomId) {
        return gameService.getRoomSelectedRoles(roomId);
    }

    // 查询当前房间游戏阶段
    @GetMapping("/stage")
    public ScriptKillGameService.GameStage getStage(@RequestParam String roomId) {
        return gameService.getCurrentStage(roomId);
    }

    // 销毁房间，释放缓存
    @DeleteMapping("/destroy")
    public String destroyRoom(@RequestParam String roomId) {
        gameService.destroyRoom(roomId);
        return "房间[" + roomId + "]缓存已全部清理完成";
    }
}