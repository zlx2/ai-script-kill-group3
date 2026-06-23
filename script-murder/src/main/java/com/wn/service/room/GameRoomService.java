/**
 * @Author: 鱼
 * @Description:
 * @DateTime: 2026/6/22 16:48
 * @Component:
 **/
package com.wn.service.room;

import com.wn.controller.room.vo.RoomDetailVO;
import com.wn.controller.room.vo.RoomPlayerVO;
import com.wn.service.exception.BusinessException;

import java.util.List;

public  interface GameRoomService{

    String createRoom(Long scriptId, String roomName, String password, Long userId) throws BusinessException;

    Object getByRoomNo(String roomNo);

    RoomDetailVO getRoomDetail(String roomId);

    RoomDetailVO getRoomDetailByNo(String roomNo);

    void transferHost(String roomId, Long targetUserId, Long userId);

    void kickPlayer(String roomId, Long targetUserId, Long userId);

    void dismissRoom(String roomId, Long userId);

    void startGame(String roomId, Long userId);

    List<RoomPlayerVO> getRoomPlayers(String roomId);

    void toggleReady(String roomId, Long userId);

    void leaveRoom(String roomId, Long userId);

    String joinRoom(String roomNo, String password, Long userId);
}
