/**
 * @Author: 鱼
 * @Description:
 * @DateTime: 2026/6/23 11:25
 * @Component:
 **/
package com.wn.mapper.room;

import com.wn.entity.room.RoomPO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMapper extends JpaRepository<RoomPO, String> {


    boolean existsByRoomNo(String roomNo);


}
