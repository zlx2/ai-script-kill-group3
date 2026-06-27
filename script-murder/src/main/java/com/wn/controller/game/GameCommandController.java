package com.wn.controller.game;

import com.wn.game.CommandType;
import com.wn.game.CommandResult;
import com.wn.game.RoomCommand;
import com.wn.game.RoomCommandService;
import com.wn.entity.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 统一游戏命令入口控制器
 */
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameCommandController {

    private final RoomCommandService commandService;

    /**
     * 统一命令入口
     * POST /api/game/command
     */
    @PostMapping("/command")
    public R executeCommand(
            @RequestHeader("userId") Long userId,
            @RequestBody Map<String, Object> body) {

        String typeStr = (String) body.get("type");
        if (typeStr == null) {
            return R.error("缺少命令类型");
        }

        CommandType type;
        try {
            type = CommandType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return R.error("未知命令类型: " + typeStr);
        }

        String roomId = (String) body.get("roomId");
        if (roomId == null) {
            return R.error("缺少房间ID");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) body.get("payload");

        RoomCommand command = new RoomCommand(
                UUID.randomUUID().toString(),
                roomId,
                userId,
                type,
                payload
        );

        try {
            CommandResult result = commandService.handle(command);
            if (result.success()) {
                return R.success(result.message());
            } else {
                return R.error(result.message());
            }
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
}
