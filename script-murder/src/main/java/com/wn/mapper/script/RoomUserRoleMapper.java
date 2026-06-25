/**
 * @Author: 弗
 * @Description: 
 * @DateTime: 2026/6/25 11:15
 * @Component: 
 **/
package com.wn.mapper.script;


import com.wn.entity.script.stage.RoomUserRolePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoomUserRoleMapper extends JpaRepository<RoomUserRolePO, Long> {
    Optional<RoomUserRolePO> findByRoomIdAndUserId(String roomId, Long userId);
}
