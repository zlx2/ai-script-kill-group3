package com.wn.game.handler;

import com.wn.entity.dm.RoomCluePO;
import com.wn.entity.game.GameEventPO;
import com.wn.entity.room.RoomPO;
import com.wn.entity.room.RoomPlayerPO;
import com.wn.game.*;
import com.wn.mapper.dm.DmRoomClueMapper;
import com.wn.mapper.room.RoomPlayerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 搜证处理器 — 玩家发现一条线索
 */
@Component
@RequiredArgsConstructor
public class SearchClueHandler implements RoomCommandHandler {

    private final DmRoomClueMapper dmRoomClueMapper;
    private final RoomPlayerMapper roomPlayerMapper;

    @Override
    public CommandType type() {
        return CommandType.SEARCH_CLUE;
    }

    @Override
    public Set<GamePhase> allowedPhases() {
        return Set.of(GamePhase.SEARCHING);
    }

    @Override
    public CommandResult handle(RoomPO room, RoomCommand command) {
        Long clueId = command.payload() != null
                ? Long.valueOf(command.payload().get("clueId").toString())
                : null;
        if (clueId == null) {
            return CommandResult.fail("缺少线索ID");
        }

        // 检查玩家是否在房间
        RoomPlayerPO player = roomPlayerMapper
                .findByRoomIdAndUserIdAndLeaveTimeIsNull(room.getRoomId(), command.userId());
        if (player == null) {
            return CommandResult.fail("玩家不在房间内");
        }

        // 创建房间线索（默认未公开）
        RoomCluePO roomClue = new RoomCluePO();
        roomClue.setRoomId(room.getRoomId());
        roomClue.setClueId(clueId);
        roomClue.setPlayerId(command.userId());
        roomClue.setIsPublic(0);
        dmRoomClueMapper.save(roomClue);

        Map<String, Object> payload = new HashMap<>();
        payload.put("clueId", clueId);
        payload.put("userId", command.userId());

        GameEventPO event = new GameEventPO();
        event.setRoomId(room.getRoomId());
        event.setUserId(command.userId());
        event.setEventType("CLUE_FOUND");
        event.setVisibility("PRIVATE");
        event.setTargetUserId(command.userId());
        event.setPayloadJson(new com.alibaba.fastjson2.JSONObject(payload).toString());

        return CommandResult.ok("线索已获取", List.of(event));
    }
}
