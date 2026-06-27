package com.wn.mapper.game;

import com.wn.entity.game.RoomVotePO;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoomVoteRepository extends JpaRepository<RoomVotePO, Long> {
    List<RoomVotePO> findByRoomId(String roomId);
    Optional<RoomVotePO> findByRoomIdAndUserId(String roomId, Long userId);
    long countByRoomId(String roomId);
}
