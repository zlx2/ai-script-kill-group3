package com.wn.game;

/**
 * 房间命令类型 — 所有改变游戏状态的操作都走这里
 */
public enum CommandType {
    START_GAME,
    SELECT_ROLE,
    STAGE_READY,
    ADVANCE_PHASE,
    SEARCH_CLUE,
    OPEN_CLUE,
    SEND_CHAT,
    SUBMIT_VOTE,
    AI_DM_ACTION
}
