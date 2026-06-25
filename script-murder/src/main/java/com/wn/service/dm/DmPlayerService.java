package com.wn.service.dm;

import com.wn.entity.dm.DmPlayerTaskPO;

public interface DmPlayerService {

    void assignRole(String roomId, Long playerId, Long roleId);

    void mutePlayer(String roomId, Long playerId);
    void unmutePlayer(String roomId, Long playerId);
    boolean isMuted(String roomId, Long playerId);

    DmPlayerTaskPO getPlayerTask(String roomId, Long playerId);
    void updatePlayerTask(String roomId, Long playerId, String taskProgress);
}