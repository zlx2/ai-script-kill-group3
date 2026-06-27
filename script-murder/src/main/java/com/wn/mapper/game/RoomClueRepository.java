package com.wn.mapper.game;

import com.wn.entity.game.RoomCluePO;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomClueRepository extends JpaRepository<RoomCluePO, Long> {
    List<RoomCluePO> findByRoomId(String roomId);
    List<RoomCluePO> findByRoomIdAndVisibility(String roomId, String visibility);
    List<RoomCluePO> findByRoomIdAndDiscoveredBy(String roomId, Long userId);
}
