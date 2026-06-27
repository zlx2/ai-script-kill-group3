package com.wn.game.handler;

import com.wn.entity.game.GameEventPO;
import com.wn.entity.room.RoomPO;
import com.wn.game.*;
import com.wn.mapper.room.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 推进阶段处理器
 */
@Component
@RequiredArgsConstructor
public class AdvancePhaseHandler implements RoomCommandHandler {

    private static final List<GamePhase> PHASE_ORDER = List.of(
            GamePhase.WAITING,
            GamePhase.SELECT_ROLE,
            GamePhase.READING,
            GamePhase.DISCUSSION_1,
            GamePhase.SEARCHING,
            GamePhase.DISCUSSION_2,
            GamePhase.VOTING,
            GamePhase.REVIEW,
            GamePhase.FINISHED
    );

    private final RoomMapper roomMapper;

    @Override
    public CommandType type() {
        return CommandType.ADVANCE_PHASE;
    }

    @Override
    public Set<GamePhase> allowedPhases() {
        return Set.of(
                GamePhase.READING,
                GamePhase.DISCUSSION_1,
                GamePhase.SEARCHING,
                GamePhase.DISCUSSION_2,
                GamePhase.VOTING,
                GamePhase.REVIEW
        );
    }

    @Override
    public CommandResult handle(RoomPO room, RoomCommand command) {
        // 只有房主能推进
        if (!room.getHostId().equals(command.userId())) {
            return CommandResult.fail("只有房主才能推进阶段");
        }

        GamePhase current = GamePhase.valueOf(room.getCurrentStage().toUpperCase());
        int idx = PHASE_ORDER.indexOf(current);
        if (idx < 0 || idx >= PHASE_ORDER.size() - 1) {
            return CommandResult.fail("无法继续推进");
        }

        GamePhase next = PHASE_ORDER.get(idx + 1);

        // 更新房间
        room.setCurrentStage(next.name().toLowerCase());
        roomMapper.save(room);

        Map<String, Object> payload = new HashMap<>();
        payload.put("phase", next.name().toLowerCase());
        payload.put("previousPhase", current.name().toLowerCase());

        GameEventPO event = new GameEventPO();
        event.setRoomId(room.getRoomId());
        event.setUserId(command.userId());
        event.setEventType("PHASE_CHANGED");
        event.setVisibility("PUBLIC");
        event.setPayloadJson(new com.alibaba.fastjson2.JSONObject(payload).toString());

        return CommandResult.ok("阶段已推进到: " + next, List.of(event));
    }
}
