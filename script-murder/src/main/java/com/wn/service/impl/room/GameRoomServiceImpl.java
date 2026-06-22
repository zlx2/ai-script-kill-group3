/**
 * @Author: 鱼
 * @Description:
 * @DateTime: 2026/6/22 17:21
 * @Component:
 **/
package com.wn.service.impl.room;

import com.wn.controller.room.vo.RoomDetailVO;
import com.wn.controller.room.vo.RoomPlayerVO;
import com.wn.service.room.GameRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameRoomServiceImpl implements GameRoomService {
    /**
     * 创建房间
     */
    @Override
    public String createRoom(Long scriptId, String roomName, String password, Long userId) {
        return "";
    }
    /**
     * 根据房间号查房间
     */
    @Override
    public Object getByRoomNo(String roomNo) {

        return null;
    }

    @Override
    public RoomDetailVO getRoomDetail(String roomId) {
        return null;
    }

    @Override
    public RoomDetailVO getRoomDetailByNo(String roomNo) {
        return null;
    }

    @Override
    public void transferHost(String roomId, Long targetUserId, Long userId) {

    }

    @Override
    public void kickPlayer(String roomId, Long targetUserId, Long userId) {

    }

    @Override
    public void dismissRoom(String roomId, Long userId) {

    }

    @Override
    public void startGame(String roomId, Long userId) {

    }

    @Override
    public List<RoomPlayerVO> getRoomPlayers(String roomId) {
        return List.of();
    }

    @Override
    public void toggleReady(String roomId, Long userId) {

    }

    @Override
    public void leaveRoom(String roomId, Long userId) {

    }

    @Override
    public String joinRoom(String roomNo, String password, Long userId) {
        return "";
    }
}
