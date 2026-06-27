package com.wn.game;

import com.wn.entity.game.GameEventPO;
import com.wn.entity.room.RoomPO;
import com.wn.mapper.game.GameEventRepository;
import com.wn.mapper.room.RoomMapper;
import com.wn.websocket.WebSocketHandler;
import com.wn.websocket.vo.WsMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 统一游戏命令入口 — 所有改变游戏状态的操作都经过这里
 *
 * 流程：
 * 1. 防重复提交（requestId 去重）
 * 2. 锁住 roomId（FOR UPDATE）
 * 3. 读取房间状态
 * 4. 找到对应的 Handler
 * 5. 检查阶段是否允许
 * 6. 执行业务逻辑
 * 7. 保存 GameEvent
 * 8. 返回事件列表供调用方广播
 */
@Component
@RequiredArgsConstructor
public class RoomCommandService {

    private final RoomMapper roomMapper;
    private final GameEventRepository eventRepository;
    private final WebSocketHandler webSocketHandler;
    private final List<RoomCommandHandler> handlers;

    /** 已处理的 requestId 去重 */
    private final Set<String> processedRequests = Collections.synchronizedSet(new HashSet<>());

    @Transactional
    public CommandResult handle(RoomCommand command) {
        // 1. 防重复提交
        if (command.requestId() != null && !processedRequests.add(command.requestId())) {
            return CommandResult.fail("重复请求");
        }

        // 2. 锁住房间
        RoomPO room = roomMapper.findByIdForUpdate(command.roomId())
                .orElseThrow(() -> new IllegalStateException("房间不存在"));

        // 3. 找到处理器
        RoomCommandHandler handler = handlers.stream()
                .filter(h -> h.type() == command.type())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知命令类型: " + command.type()));

        // 4. 检查阶段是否允许
        GamePhase currentPhase = GamePhase.valueOf(room.getCurrentStage().toUpperCase());
        Set<GamePhase> allowed = handler.allowedPhases();
        if (!allowed.contains(currentPhase)) {
            return CommandResult.fail("当前阶段不允许此操作: " + currentPhase);
        }

        // 5. 执行
        CommandResult result = handler.handle(room, command);

        // 6. 保存并广播事件
        if (result.events() != null) {
            for (GameEventPO event : result.events()) {
                eventRepository.save(event);
                broadcastEvent(event);
            }
        }

        return result;
    }

    private void broadcastEvent(GameEventPO event) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getId());
        data.put("roomId", event.getRoomId());
        data.put("senderId", event.getUserId());
        data.put("visibility", event.getVisibility());
        data.put("payload", event.getPayloadJson());
        data.put("serverTime", System.currentTimeMillis());

        WsMessage<Map<String, Object>> msg = WsMessage.<Map<String, Object>>builder()
                .type(event.getEventType())
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();

        if ("PRIVATE".equals(event.getVisibility()) && event.getTargetUserId() != null) {
            webSocketHandler.sendToUser(event.getTargetUserId(), msg);
        } else {
            webSocketHandler.broadcastToRoom(event.getRoomId(), msg);
        }
    }
}
