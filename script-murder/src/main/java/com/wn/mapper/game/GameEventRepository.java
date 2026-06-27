package com.wn.mapper.game;

import com.wn.entity.game.GameEventPO;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GameEventRepository extends JpaRepository<GameEventPO, Long> {
    List<GameEventPO> findByRoomIdOrderByCreatedAtAsc(String roomId);

    List<GameEventPO> findByRoomIdAndIdGreaterThanOrderByCreatedAtAsc(String roomId, Long lastEventId);
}
