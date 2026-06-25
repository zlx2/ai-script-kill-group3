package com.wn.mapper.dm;

import com.wn.entity.dm.RoomCluePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: 杜江
 * @Description: dm房间线索实体
 * @DateTime: 2026/6/25 11:32
 * @Component:
 **/
@Repository
public interface DmRoomClueMapper extends JpaRepository<RoomCluePO, Long> {
    List<RoomCluePO> findByRoomId(String roomId);
    List<RoomCluePO> findByRoomIdAndPlayerId(String roomId, Long playerId);
    boolean existsByRoomIdAndPlayerIdAndClueId(String roomId, Long playerId, Long clueId);
}
