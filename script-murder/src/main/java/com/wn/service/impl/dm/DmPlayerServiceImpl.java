/**
 * @Author: 杜江
 * @Description: dm玩家服务实现类
 * @DateTime: 2026/6/25 12:11
 * @Component:
 **/
package com.wn.service.impl.dm;

import com.wn.entity.dm.DmPlayerTaskPO;
import com.wn.entity.room.RoomPlayerPO;
import com.wn.entity.script.stage.RoomUserRolePO;
import com.wn.mapper.dm.DmPlayerTaskMapper;
import com.wn.mapper.room.RoomPlayerMapper;
import com.wn.mapper.script.RoomUserRoleMapper;
import com.wn.service.dm.DmPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DmPlayerServiceImpl implements DmPlayerService {

    // 注入队友的 Mapper
    private final RoomUserRoleMapper roomUserRoleMapper;

    private final RoomPlayerMapper roomPlayerMapper;
    private final DmPlayerTaskMapper playerTaskMapper;

    @Override
    @Transactional
    public void assignRole(String roomId, Long playerId, Long roleId, Long scriptId) {
        // 1. 修改 RoomPlayerPO 的 roleId（兼容原有逻辑）
        RoomPlayerPO player = roomPlayerMapper
                .findByRoomIdAndUserIdAndLeaveTimeIsNull(roomId, playerId);
        if (player != null) {
            player.setRoleId(roleId);
            roomPlayerMapper.save(player);
        }

        // 2. 同时创建 RoomUserRolePO（使用队友的表）
        RoomUserRolePO roomUserRole = new RoomUserRolePO();
        roomUserRole.setRoomId(roomId);
        roomUserRole.setUserId(playerId);
        roomUserRole.setScriptId(scriptId);
        roomUserRole.setRoleId(roleId);
        roomUserRoleMapper.save(roomUserRole);
    }

    @Override
    @Transactional
    public void mutePlayer(String roomId, Long playerId) {
        DmPlayerTaskPO task = getOrCreatePlayerTask(roomId, playerId);
        task.setIsMuted((byte) 1);
        playerTaskMapper.save(task);
    }

    @Override
    @Transactional
    public void unmutePlayer(String roomId, Long playerId) {
        DmPlayerTaskPO task = getOrCreatePlayerTask(roomId, playerId);
        task.setIsMuted((byte) 0);
        playerTaskMapper.save(task);
    }

    @Override
    public boolean isMuted(String roomId, Long playerId) {
        DmPlayerTaskPO task = playerTaskMapper
                .findByRoomIdAndPlayerId(roomId, playerId).orElse(null);
        return task != null && task.getIsMuted() == 1;
    }

    @Override
    public DmPlayerTaskPO getPlayerTask(String roomId, Long playerId) {
        return playerTaskMapper.findByRoomIdAndPlayerId(roomId, playerId).orElse(null);
    }

    @Override
    @Transactional
    public void updatePlayerTask(String roomId, Long playerId, String taskProgress) {
        DmPlayerTaskPO task = getOrCreatePlayerTask(roomId, playerId);
        task.setTaskProgress(taskProgress);
        playerTaskMapper.save(task);
    }

    private DmPlayerTaskPO getOrCreatePlayerTask(String roomId, Long playerId) {
        return playerTaskMapper.findByRoomIdAndPlayerId(roomId, playerId)
                .orElseGet(() -> {
                    DmPlayerTaskPO newTask = new DmPlayerTaskPO();
                    newTask.setRoomId(roomId);
                    newTask.setPlayerId(playerId);
                    return newTask;
                });
    }
}
