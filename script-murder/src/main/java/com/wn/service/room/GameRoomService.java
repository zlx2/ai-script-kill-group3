package com.wn.service.room;

import com.wn.controller.room.vo.RoomDetailVO;
import com.wn.controller.room.vo.RoomPlayerVO;
import com.wn.entity.room.RoomPO;
import com.wn.service.exception.BusinessException;

import java.util.List;

public  interface GameRoomService{

    String createRoom(Long scriptId, String roomName, String password, Long userId) throws BusinessException;

    Object getByRoomNo(String roomNo);

    RoomDetailVO getRoomDetail(String roomId) throws BusinessException;

    RoomDetailVO getRoomDetailByNo(String roomNo) throws BusinessException;

    void transferHost(String roomId, Long targetUserId, Long userId) throws BusinessException;

    void kickPlayer(String roomId, Long targetUserId, Long userId) throws BusinessException;

    void dismissRoom(String roomId, Long userId) throws BusinessException;

    void startGame(String roomId, Long userId) throws BusinessException;

    List<RoomPlayerVO> getRoomPlayers(String roomId);

    void toggleReady(String roomId, Long userId) throws BusinessException;

    void leaveRoom(String roomId, Long userId);

    String joinRoom(String roomNo, String password, Long userId) throws BusinessException;

    RoomPO getByRoomId(String roomId);

    RoomPO findByRoomNo(String roomNo) throws BusinessException;

    List<RoomDetailVO> getRoomList();
}
