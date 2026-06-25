package com.wn.service.clue;

import com.wn.entity.clue.ScriptCluePO;

import java.util.List;

public interface ClueService {

    List<ScriptCluePO> getAllClues(Long roleId, String scene);

    void openClue(Long clueId);
}
