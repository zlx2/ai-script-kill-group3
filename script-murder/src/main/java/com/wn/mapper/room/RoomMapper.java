/**
 * @Author: 鱼
 * @Description:
 * @DateTime: 2026/6/23 11:25
 * @Component:
 **/
package com.wn.mapper.room;

import com.wn.entity.room.RoomPO;
import io.fabric8.kubernetes.api.model.authentication.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMapper extends JpaRepository<RoomPO,Long> {


    boolean existsByRoomNo(String roomNo);
}
