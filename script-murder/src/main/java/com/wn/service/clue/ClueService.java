package com.wn.service.clue;

import com.wn.entity.clue.ScriptCluePO;
import com.wn.entity.dm.RoomCluePO;

import java.util.List;

public interface ClueService {
    List<ScriptCluePO> getAllClues(Long roleId, int scene, String roomId);
    List<ScriptCluePO> openClue(String roomId, Long clueId, Long userId, Long roleId, int scene);

    void addClue(ScriptCluePO scriptCluePO);
}
