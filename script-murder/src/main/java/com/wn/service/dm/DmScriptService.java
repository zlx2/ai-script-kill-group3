/**
 * @Author: 杜江
 * @Description: 剧本手册管理
 * @DateTime: 2026/6/25 11:57
 * @Component:
 **/
package com.wn.service.dm;

import com.wn.entity.dm.*;
import com.wn.entity.script.stage.ScriptStagePO;

import java.util.List;

public interface DmScriptService {

    List<ScriptStagePO> getScriptStages(Long scriptId);

    void saveScriptHints(Long scriptId, List<ScriptHintPO> hints);
    List<ScriptHintPO> getScriptHints(Long scriptId);
    List<ScriptHintPO> getScriptHintsByLevel(Long scriptId, Byte level);

    void saveScriptReview(Long scriptId, ScriptReviewPO review);
    ScriptReviewPO getScriptReview(Long scriptId);
}