package com.wn.game;

import com.wn.entity.room.RoomPO;
import java.util.Set;

/**
 * 房间命令处理器 — 每个 CommandType 对应一个 Handler
 */
public interface RoomCommandHandler {

    /** 处理器负责的命令类型 */
    CommandType type();

    /** 该命令允许执行的阶段 */
    Set<GamePhase> allowedPhases();

    /** 核心处理方法 */
    CommandResult handle(RoomPO room, RoomCommand command);
}
