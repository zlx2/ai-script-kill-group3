/**
 * @Author: 鱼
 * @Description:
 * @DateTime: 2026/6/23 11:25
 * @Component:
 **/
package com.wn.mapper.room;

import com.wn.entity.room.RoomPO;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomMapper extends JpaRepository<RoomPO, String> {

    boolean existsByRoomNo(String roomNo);

    Optional<RoomPO> findByRoomNo(String roomNo);

    /** 加悲观写锁查询房间 — 多人游戏防并发 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RoomPO r where r.roomId = :roomId")
    Optional<RoomPO> findByIdForUpdate(@Param("roomId") String roomId);
}
