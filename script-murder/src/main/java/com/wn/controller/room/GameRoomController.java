package com.wn.controller.room;

import com.wn.controller.room.dto.CreateRoomDTO;
import com.wn.controller.room.dto.JoinRoomDTO;
import com.wn.controller.room.vo.CreateRoomResultVO;
import com.wn.controller.room.vo.RoomDetailVO;
import com.wn.controller.room.vo.RoomPlayerVO;
import com.wn.entity.R;
import com.wn.service.room.GameRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 游戏房间控制器
 * @Author: 鱼
 * @Description: 剧本杀房间相关接口
 * @DateTime: 2026/6/22
 * @Component: room模块控制器
 **/
@RestController
@RequestMapping("/api/game/room")
@RequiredArgsConstructor
public class GameRoomController {

    private final GameRoomService gameRoomService;

    /**
     * 创建房间
     * POST /api/game/room/create
     * 前端剧本详情页点击创建房间调用
     * @param dto 创建房间参数
     * @param userId 当前登录用户ID（请求头）
     * @return 房间基础信息roomNo、roomId、roomName
     */
    @PostMapping("/create")
    public R createRoom(
            @RequestBody CreateRoomDTO dto,
            @RequestHeader("userId") Long userId) {

        String roomNo = gameRoomService.createRoom(
                dto.getScriptId(), dto.getRoomName(), dto.getPassword(), userId
        );

        CreateRoomResultVO result = new CreateRoomResultVO();
        result.setRoomNo(roomNo);
//        result.setRoomId(gameRoomService.getByRoomNo(roomNo).getRoomId());
        result.setRoomName(dto.getRoomName());

        // 携带数据返回成功
        return new R(result);
    }

    /**
     * 根据roomId查询房间详情
     * @param roomId 房间UUID主键
     * @return 房间完整详情VO
     */
    @GetMapping("/{roomId}")
    public R getRoomDetail(@PathVariable String roomId) {
        RoomDetailVO detailVO = gameRoomService.getRoomDetail(roomId);
        return new R(detailVO);
    }

    /**
     * 根据房间号roomNo查询房间
     * @param roomNo 6位房间编号
     * @return 房间完整详情VO
     */
    @GetMapping("/info")
    public R getRoomByNo(@RequestParam String roomNo) {
        RoomDetailVO detailVO = gameRoomService.getRoomDetailByNo(roomNo);
        return new R(detailVO);
    }

    /**
     * 玩家加入房间
     * @param dto 房间号+密码
     * @param userId 当前玩家ID
     * @return 房间UUID roomId
     */
    @PostMapping("/join")
    public R joinRoom(
            @RequestBody JoinRoomDTO dto,
            @RequestHeader("userId") Long userId) {

        String roomId = gameRoomService.joinRoom(dto.getRoomNo(), dto.getPassword(), userId);
        return new R(roomId);
    }

    /**
     * 玩家退出房间
     * @param roomId 房间ID
     * @param userId 当前玩家ID
     */
    @PostMapping("/leave")
    public R leaveRoom(
            @RequestParam String roomId,
            @RequestHeader("userId") Long userId) {

        gameRoomService.leaveRoom(roomId, userId);
        // 无返回数据，直接返回全局成功常量
        return R.SUCCESS;
    }

    /**
     * 切换准备状态
     * @param roomId 房间ID
     * @param userId 当前玩家ID
     */
    @PostMapping("/ready")
    public R toggleReady(
            @RequestParam String roomId,
            @RequestHeader("userId") Long userId) {

        gameRoomService.toggleReady(roomId, userId);
        return R.SUCCESS;
    }

    /**
     * 获取房间内所有玩家列表
     * @param roomId 房间ID
     * @return 玩家信息集合
     */
    @GetMapping("/{roomId}/players")
    public R getPlayers(@PathVariable String roomId) {
        List<RoomPlayerVO> playerList = gameRoomService.getRoomPlayers(roomId);
        return new R(playerList);
    }

    /**
     * 房主开局
     * @param roomId 房间ID
     * @param userId 房主ID
     */
    @PostMapping("/start")
    public R startGame(
            @RequestParam String roomId,
            @RequestHeader("userId") Long userId) {

        gameRoomService.startGame(roomId, userId);
        return R.SUCCESS;
    }

    /**
     * 房主解散房间
     * @param roomId 房间ID
     * @param userId 房主ID
     */
    @PostMapping("/dismiss")
    public R dismissRoom(
            @RequestParam String roomId,
            @RequestHeader("userId") Long userId) {

        gameRoomService.dismissRoom(roomId, userId);
        return R.SUCCESS;
    }

    /**
     * 房主踢出玩家
     * @param roomId 房间ID
     * @param targetUserId 被踢玩家ID
     * @param userId 当前房主ID
     */
    @PostMapping("/kick")
    public R kickPlayer(
            @RequestParam String roomId,
            @RequestParam Long targetUserId,
            @RequestHeader("userId") Long userId) {

        gameRoomService.kickPlayer(roomId, targetUserId, userId);
        return R.SUCCESS;
    }

    /**
     * 转让房主权限
     * @param roomId 房间ID
     * @param targetUserId 接收房主的玩家ID
     * @param userId 当前原房主ID
     */
    @PostMapping("/transfer")
    public R transferHost(
            @RequestParam String roomId,
            @RequestParam Long targetUserId,
            @RequestHeader("userId") Long userId) {

        gameRoomService.transferHost(roomId, targetUserId, userId);
        return R.SUCCESS;
    }
}