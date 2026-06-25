package com.wn.mapper.dm;

import com.wn.entity.dm.RoomVotePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: 杜江
 * @Description: dm对局房间玩家投票记录表
 * @DateTime: 2026/6/25 11:33
 * @Component:
 **/
@Repository
public interface DmRoomVoteMapper extends JpaRepository<RoomVotePO, Long> {
    List<RoomVotePO> findByRoomId(String roomId);
    boolean existsByRoomIdAndPlayerId(String roomId, Long playerId);
}