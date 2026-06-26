/**
 * @Author: 杜江
 * @Description: dm剧本服务实现类
 * @DateTime: 2026/6/25 12:02
 * @Component:
 **/
package com.wn.service.impl.dm;

import com.wn.entity.dm.ScriptHintPO;
import com.wn.entity.dm.ScriptReviewPO;
import com.wn.entity.script.stage.ScriptStagePO;
import com.wn.mapper.dm.DmScriptHintMapper;
import com.wn.mapper.dm.DmScriptReviewMapper;
import com.wn.mapper.script.ScriptStageMapper;
import com.wn.service.dm.DmScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DmScriptServiceImpl implements DmScriptService {

    private final ScriptStageMapper stageMapper;

    private final DmScriptHintMapper hintMapper;
    private final DmScriptReviewMapper reviewMapper;

    @Override
    public List<ScriptStagePO> getScriptStages(Long scriptId) {
        return stageMapper.findByScriptIdOrderByStageNoAsc(scriptId);
    }

    @Override
    @Transactional
    public void saveScriptHints(Long scriptId, List<ScriptHintPO> hints) {
        hints.forEach(hint -> hint.setScriptId(scriptId));
        hintMapper.saveAll(hints);
    }

    @Override
    public List<ScriptHintPO> getScriptHints(Long scriptId) {
        return hintMapper.findByScriptId(scriptId);
    }

    @Override
    public List<ScriptHintPO> getScriptHintsByLevel(Long scriptId, Byte level) {
        return hintMapper.findByScriptIdAndHintLevel(scriptId, level);
    }

    @Override
    @Transactional
    public void saveScriptReview(Long scriptId, ScriptReviewPO review) {
        review.setScriptId(scriptId);
        reviewMapper.save(review);
    }

    @Override
    public ScriptReviewPO getScriptReview(Long scriptId) {
        return reviewMapper.findByScriptId(scriptId).orElse(null);
    }
}
