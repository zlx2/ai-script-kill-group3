package com.wn.controller.game;

import com.wn.entity.R;
import com.wn.entity.game.GameEventPO;
import com.wn.entity.room.RoomPO;
import com.wn.mapper.game.GameEventRepository;
import com.wn.mapper.room.RoomMapper;
import com.wn.mapper.room.RoomPlayerMapper;
import com.wn.service.room.GameRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 同步/恢复接口 — 玩家断线重连后调用
 */
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class SyncController {

    private final GameEventRepository eventRepository;
    private final RoomMapper roomMapper;
    private final RoomPlayerMapper roomPlayerMapper;
    private final GameRoomService gameRoomService;

    /**
     * 获取房间完整状态 + 最新事件
     * GET /api/game/sync?roomId=xxx&lastEventId=123
     */
    @GetMapping("/sync")
    public R sync(
            @RequestParam String roomId,
            @RequestParam(required = false) Long lastEventId) {

        RoomPO room = roomMapper.findById(roomId).orElse(null);
        if (room == null) {
            return R.error("房间不存在");
        }

        Map<String, Object> result = new HashMap<>();

        // 房间基础状态
        Map<String, Object> roomState = new HashMap<>();
        roomState.put("roomId", room.getRoomId());
        roomState.put("roomStatus", room.getRoomStatus());
        roomState.put("currentStage", room.getCurrentStage());
        roomState.put("currentRound", room.getCurrentRound());
        roomState.put("scriptId", room.getScriptId());
        roomState.put("hostId", room.getHostId());
        result.put("roomState", roomState);

        // 玩家列表
        result.put("players", gameRoomService.getRoomPlayers(roomId));

        // 错过的消息事件
        List<GameEventPO> missedEvents;
        if (lastEventId != null && lastEventId > 0) {
            missedEvents = eventRepository.findByRoomIdAndIdGreaterThanOrderByCreatedAtAsc(roomId, lastEventId);
        } else {
            // 第一次连接或无法获取 lastEventId 时，只返回最近 N 条
            List<GameEventPO> all = eventRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
            int from = Math.max(0, all.size() - 50);
            missedEvents = all.subList(from, all.size());
        }

        List<Map<String, Object>> eventList = new ArrayList<>();
        for (GameEventPO e : missedEvents) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("eventId", e.getId());
            ev.put("type", e.getEventType());
            ev.put("userId", e.getUserId());
            ev.put("visibility", e.getVisibility());
            ev.put("payload", e.getPayloadJson());
            ev.put("serverTime", e.getCreatedAt());
            eventList.add(ev);
        }
        result.put("missedEvents", eventList);
        result.put("serverTime", System.currentTimeMillis());

        return R.success(result);
    }
}
