package com.wn.game.handler;

import com.wn.entity.game.GameEventPO;
import com.wn.entity.game.RoomCluePO;
import com.wn.entity.room.RoomPO;
import com.wn.entity.room.RoomPlayerPO;
import com.wn.game.*;
import com.wn.mapper.game.RoomClueRepository;
import com.wn.mapper.room.RoomPlayerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 公开线索处理器
 */
@Component
@RequiredArgsConstructor
public class OpenClueHandler implements RoomCommandHandler {

    private final RoomClueRepository roomClueRepository;
    private final RoomPlayerMapper roomPlayerMapper;

    @Override
    public CommandType type() {
        return CommandType.OPEN_CLUE;
    }

    @Override
    public Set<GamePhase> allowedPhases() {
        return Set.of(GamePhase.SEARCHING, GamePhase.DISCUSSION_1, GamePhase.DISCUSSION_2);
    }

    @Override
    public CommandResult handle(RoomPO room, RoomCommand command) {
        Long clueId = command.payload() != null
                ? Long.valueOf(command.payload().get("clueId").toString())
                : null;
        if (clueId == null) {
            return CommandResult.fail("缺少线索ID");
        }

        // 查找该玩家拥有的这条线索
        List<RoomCluePO> clues = roomClueRepository.findByRoomIdAndDiscoveredBy(room.getRoomId(), command.userId());
        RoomCluePO targetClue = clues.stream()
                .filter(c -> c.getClueId().equals(clueId) && "PRIVATE".equals(c.getVisibility()))
                .findFirst().orElse(null);

        if (targetClue == null) {
            return CommandResult.fail("未找到可公开的线索");
        }

        // 公开
        targetClue.setVisibility("PUBLIC");
        targetClue.setOpenedAt(LocalDateTime.now());
        roomClueRepository.save(targetClue);

        Map<String, Object> payload = new HashMap<>();
        payload.put("clueId", clueId);
        payload.put("userId", command.userId());

        GameEventPO event = new GameEventPO();
        event.setRoomId(room.getRoomId());
        event.setUserId(command.userId());
        event.setEventType("CLUE_OPENED");
        event.setVisibility("PUBLIC");
        event.setPayloadJson(new com.alibaba.fastjson2.JSONObject(payload).toString());

        return CommandResult.ok("线索已公开", List.of(event));
    }
}
