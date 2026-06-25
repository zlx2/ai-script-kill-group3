package com.wn.mapper.dm;

import com.wn.entity.dm.DmRoomStatePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @Author: 杜江
 * @Description: dm房间状态实体
 * @DateTime: 2026/6/25 11:33
 * @Component:
 **/
@Repository
public interface DmRoomStateMapper extends JpaRepository<DmRoomStatePO, String> {
    Optional<DmRoomStatePO> findByRoomId(String roomId);
}
