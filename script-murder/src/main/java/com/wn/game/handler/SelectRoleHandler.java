package com.wn.game.handler;

import com.wn.entity.game.GameEventPO;
import com.wn.entity.room.RoomPO;
import com.wn.entity.room.RoomPlayerPO;
import com.wn.game.*;
import com.wn.mapper.room.RoomMapper;
import com.wn.mapper.room.RoomPlayerMapper;
import com.wn.mapper.script.ScriptRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 选择角色处理器
 */
@Component
@RequiredArgsConstructor
public class SelectRoleHandler implements RoomCommandHandler {

    private final RoomPlayerMapper roomPlayerMapper;
    private final RoomMapper roomMapper;

    @Override
    public CommandType type() {
        return CommandType.SELECT_ROLE;
    }

    @Override
    public Set<GamePhase> allowedPhases() {
        return Set.of(GamePhase.SELECT_ROLE);
    }

    @Override
    public CommandResult handle(RoomPO room, RoomCommand command) {
        Long roleId = command.payload() != null
                ? Long.valueOf(command.payload().get("roleId").toString())
                : null;
        if (roleId == null) {
            return CommandResult.fail("缺少角色ID");
        }

        RoomPlayerPO player = roomPlayerMapper
                .findByRoomIdAndUserIdAndLeaveTimeIsNull(room.getRoomId(), command.userId());
        if (player == null) {
            return CommandResult.fail("玩家不在房间内");
        }
        if (player.getRoleId() != null) {
            return CommandResult.fail("已选择角色，不能更改");
        }

        // 检查角色是否已被选
        List<RoomPlayerPO> players = roomPlayerMapper
                .findByRoomIdAndLeaveTimeIsNull(room.getRoomId());
        boolean taken = players.stream()
                .filter(p -> !p.getUserId().equals(command.userId()))
                .anyMatch(p -> roleId.equals(p.getRoleId()));
        if (taken) {
            return CommandResult.fail("该角色已被选择");
        }

        // 分配角色
        player.setRoleId(roleId);
        roomPlayerMapper.save(player);

        List<GameEventPO> events = new ArrayList<>();

        // 角色选择事件
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", command.userId());
        payload.put("roleId", roleId);

        GameEventPO selectEvent = new GameEventPO();
        selectEvent.setRoomId(room.getRoomId());
        selectEvent.setUserId(command.userId());
        selectEvent.setEventType("ROLE_SELECTED");
        selectEvent.setVisibility("PUBLIC");
        selectEvent.setPayloadJson(new com.alibaba.fastjson2.JSONObject(payload).toString());
        events.add(selectEvent);

        // 检查是否全部选完 → 自动推进到 READING
        boolean allSelected = players.stream()
                .allMatch(p -> p.getRoleId() != null || p.getUserId().equals(command.userId()));
        // 重新检查
        List<RoomPlayerPO> allPlayers = roomPlayerMapper
                .findByRoomIdAndLeaveTimeIsNull(room.getRoomId());
        boolean allDone = allPlayers.stream().allMatch(p -> p.getRoleId() != null);

        if (allDone) {
            room.setCurrentStage(GamePhase.READING.name().toLowerCase());
            roomMapper.save(room);

            Map<String, Object> phasePayload = new HashMap<>();
            phasePayload.put("phase", GamePhase.READING.name().toLowerCase());

            GameEventPO phaseEvent = new GameEventPO();
            phaseEvent.setRoomId(room.getRoomId());
            phaseEvent.setUserId(null);
            phaseEvent.setEventType("PHASE_CHANGED");
            phaseEvent.setVisibility("PUBLIC");
            phaseEvent.setPayloadJson(new com.alibaba.fastjson2.JSONObject(phasePayload).toString());
            events.add(phaseEvent);
        }

        return CommandResult.ok("角色选择成功", events);
    }
}
