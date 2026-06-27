-- ============================================================
-- 游戏事件表 — 所有状态变更都记录
-- ============================================================
CREATE TABLE IF NOT EXISTS game_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id VARCHAR(36) NOT NULL,
    user_id BIGINT NULL,
    event_type VARCHAR(64) NOT NULL COMMENT 'GAME_STARTED/PHASE_CHANGED/ROLE_SELECTED/CLUE_FOUND/VOTE_SUBMITTED...',
    visibility VARCHAR(32) NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC / PRIVATE',
    target_user_id BIGINT NULL COMMENT '私密事件的接收者',
    payload_json JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ge_room_id (room_id),
    INDEX idx_ge_event_type (event_type),
    INDEX idx_ge_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 房间线索状态表
-- ============================================================
CREATE TABLE IF NOT EXISTS room_clue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id VARCHAR(36) NOT NULL,
    clue_id BIGINT NOT NULL COMMENT '对应 script_clue 表',
    discovered_by BIGINT NULL COMMENT '发现线索的玩家',
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE / PUBLIC',
    opened_at DATETIME NULL COMMENT '公开时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rc_room_id (room_id),
    INDEX idx_rc_clue_id (clue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 房间投票记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS room_vote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    target_role_id BIGINT NULL COMMENT '投票目标角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rv_room_id (room_id),
    INDEX idx_rv_user_id (user_id),
    UNIQUE KEY uk_room_vote_user (room_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
