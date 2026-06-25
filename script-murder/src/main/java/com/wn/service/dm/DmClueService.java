package com.wn.service.dm;

import com.wn.entity.dm.RoomCluePO;

import java.util.List;

public interface DmClueService {

    void grantClue(String roomId, Long playerId, Long clueId);
    void grantClueToAll(String roomId, Long clueId);

    List<RoomCluePO> getPlayerClues(String roomId, Long playerId);
    List<RoomCluePO> getAllRoomClues(String roomId);

    boolean hasClue(String roomId, Long playerId, Long clueId);
}
