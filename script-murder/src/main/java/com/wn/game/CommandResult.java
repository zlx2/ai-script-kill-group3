package com.wn.game;

import com.wn.entity.game.GameEventPO;
import java.util.List;

/**
 * 命令执行结果
 */
public record CommandResult(
        boolean success,
        String message,
        List<GameEventPO> events,
        Object extraData
) {
    public static CommandResult ok(String message, List<GameEventPO> events) {
        return new CommandResult(true, message, events, null);
    }

    public static CommandResult ok(String message, List<GameEventPO> events, Object extraData) {
        return new CommandResult(true, message, events, extraData);
    }

    public static CommandResult fail(String message) {
        return new CommandResult(false, message, List.of(), null);
    }
}
