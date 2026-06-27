package com.wn.game;

/**
 * 游戏阶段枚举 — 后端唯一状态定义
 */
public enum GamePhase {
    WAITING,        // 等待玩家
    SELECT_ROLE,    // 选择角色
    READING,        // 阅读剧本
    DISCUSSION_1,   // 第一轮讨论
    SEARCHING,      // 搜证
    DISCUSSION_2,   // 第二轮讨论
    VOTING,         // 投票
    REVIEW,         // 复盘
    FINISHED        // 结束
}
