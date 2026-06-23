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

public interface RoomPlayerMapper  extends JpaRepository<RoomPO,Long> {
    void save(RoomPlayerPO player);
}
