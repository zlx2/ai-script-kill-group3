/**
 * @Author: 杜江
 * @Description: 剧本手册管理
 * @DateTime: 2026/6/25 11:57
 * @Component:
 **/
package com.wn.service.dm;

import com.wn.entity.clue.ScriptCluePO;
import com.wn.entity.dm.*;

import java.util.List;

public interface DmScriptService {

    void saveScriptActs(Long scriptId, List<ScriptActPO> acts);
    List<ScriptActPO> getScriptActs(Long scriptId);

    void saveScriptClues(Long scriptId, List<ScriptCluePO> clues);
    List<ScriptCluePO> getScriptClues(Long scriptId);
    List<ScriptCluePO> getScriptCluesByAct(Long scriptId, Integer actOrder);

    void saveScriptHints(Long scriptId, List<ScriptHintPO> hints);
    List<ScriptHintPO> getScriptHints(Long scriptId);
    List<ScriptHintPO> getScriptHintsByLevel(Long scriptId, Byte level);

    void saveScriptReview(Long scriptId, ScriptReviewPO review);
    ScriptReviewPO getScriptReview(Long scriptId);
}