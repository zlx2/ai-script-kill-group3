package com.wn.game;

import java.util.Map;

/**
 * 房间命令 — 统一的操作请求格式
 */
public record RoomCommand(
        String requestId,
        String roomId,
        Long userId,
        CommandType type,
        Map<String, Object> payload
) {}
