package com.wn.mapper.dm;

import com.wn.entity.dm.DmPlayerTaskPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @Author: 杜江
 * @Description: dm玩家任务实体
 * @DateTime: 2026/6/25 11:34
 * @Component:
 **/
@Repository
public interface DmPlayerTaskMapper extends JpaRepository<DmPlayerTaskPO, Long> {
    List<DmPlayerTaskPO> findByRoomId(String roomId);
    Optional<DmPlayerTaskPO> findByRoomIdAndPlayerId(String roomId, Long playerId);
}