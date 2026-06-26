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

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomUserRoleMapper extends JpaRepository<RoomUserRolePO, Long> {
    /**
     * 根据房间+用户查询用户分配的角色
     * @param roomId 房间id
     * @param userId 用户id
     * @return 房间用户角色关联记录
     */
    Optional<RoomUserRolePO> findByRoomIdAndUserId(String roomId, Long userId);

    /**
     * 根据房间id查询所有用户角色分配
     */
    List<RoomUserRolePO> findByRoomId(String roomId);
}
