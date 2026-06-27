package com.wn.game.handler;

import com.wn.entity.game.GameEventPO;
import com.wn.entity.room.RoomPO;
import com.wn.entity.room.RoomPlayerPO;
import com.wn.game.*;
import com.wn.mapper.room.RoomMapper;
import com.wn.mapper.room.RoomPlayerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 开始游戏处理器
 */
@Component
@RequiredArgsConstructor
public class StartGameHandler implements RoomCommandHandler {

    private final RoomPlayerMapper roomPlayerMapper;
    private final RoomMapper roomMapper;

    @Override
    public CommandType type() {
        return CommandType.START_GAME;
    }

    @Override
    public Set<GamePhase> allowedPhases() {
        return Set.of(GamePhase.WAITING);
    }

    @Override
    public CommandResult handle(RoomPO room, RoomCommand command) {
        // 只有房主能开始
        if (!room.getHostId().equals(command.userId())) {
            return CommandResult.fail("只有房主才能开始游戏");
        }

        // 至少2人
        long playerCount = roomPlayerMapper.findByRoomIdAndLeaveTimeIsNull(room.getRoomId()).size();
        if (playerCount < 2) {
            return CommandResult.fail("至少需要2名玩家才能开始");
        }

        // 所有人都准备
        long readyCount = roomPlayerMapper.countByRoomIdAndIsReadyAndLeaveTimeIsNull(room.getRoomId(), (byte) 1);
        if (readyCount < playerCount) {
            return CommandResult.fail("还有玩家未准备");
        }

        // 更新房间状态
        room.setRoomStatus((byte) 1); // PLAYING
        room.setCurrentStage(GamePhase.SELECT_ROLE.name().toLowerCase());
        room.setCurrentRound(1);
        roomMapper.save(room);

        // 生成事件
        Map<String, Object> payload = new HashMap<>();
        payload.put("phase", GamePhase.SELECT_ROLE.name().toLowerCase());
        payload.put("round", 1);

        GameEventPO event = new GameEventPO();
        event.setRoomId(room.getRoomId());
        event.setUserId(command.userId());
        event.setEventType("GAME_STARTED");
        event.setVisibility("PUBLIC");
        event.setPayloadJson(convertPayload(payload));

        return CommandResult.ok("游戏开始", List.of(event));
    }

    private String convertPayload(Map<String, Object> payload) {
        try {
            return new com.alibaba.fastjson2.JSONObject(payload).toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
