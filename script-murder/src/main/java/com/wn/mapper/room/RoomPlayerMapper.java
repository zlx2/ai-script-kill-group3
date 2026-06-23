/**
 * @Author: 鱼
 * @Description:
 * @DateTime: 2026/6/23 11:26
 * @Component:
 **/
package com.wn.mapper.room;

import com.wn.entity.room.RoomPO;
import com.wn.entity.room.RoomPlayerPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomPlayerMapper extends JpaRepository<RoomPlayerPO, Long> {

    boolean existsByRoomIdAndUserIdAndLeaveTimeIsNull(String roomId, Long userId);

    RoomPlayerPO findByRoomIdAndUserIdAndLeaveTimeIsNull(String roomId, Long userId);

    long countByRoomIdAndIsReadyAndLeaveTimeIsNull(String roomId, byte b);

    List<RoomPlayerPO> findByRoomIdAndLeaveTimeIsNull(String roomId);

    RoomPlayerPO findFirstByRoomIdAndLeaveTimeIsNullOrderByJoinTimeAsc(String roomId);

    List<RoomPlayerPO> findByRoomIdAndLeaveTimeIsNullOrderByJoinTimeAsc(String roomId);
}
