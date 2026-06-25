/**
 * @Author: 杜江
 * @Description: dm剧本服务实现类
 * @DateTime: 2026/6/25 12:02
 * @Component:
 **/
package com.wn.service.impl.dm;

import com.wn.entity.clue.ScriptCluePO;
import com.wn.entity.dm.ScriptActPO;
import com.wn.entity.dm.ScriptHintPO;
import com.wn.entity.dm.ScriptReviewPO;
import com.wn.mapper.dm.DmScriptActMapper;
import com.wn.mapper.dm.DmScriptClueMapper;
import com.wn.mapper.dm.DmScriptHintMapper;
import com.wn.mapper.dm.DmScriptReviewMapper;
import com.wn.service.dm.DmScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DmScriptServiceImpl implements DmScriptService {

    private final DmScriptActMapper actMapper;
    private final DmScriptClueMapper clueMapper;
    private final DmScriptHintMapper hintMapper;
    private final DmScriptReviewMapper reviewMapper;

    /// 保存剧本动作
    @Override
    @Transactional
    public void saveScriptActs(Long scriptId, List<ScriptActPO> acts) {
        acts.forEach(act -> act.setScriptId(scriptId));
        actMapper.saveAll(acts);
    }

    /// 获取剧本动作
    @Override
    public List<ScriptActPO> getScriptActs(Long scriptId) {
        return actMapper.findByScriptIdOrderByActOrderAsc(scriptId);
    }

    /// 保存剧本线索
    @Override
    @Transactional
    public void saveScriptClues(Long scriptId, List<ScriptCluePO> clues) {
        clues.forEach(clue -> clue.setScriptId(scriptId));
        clueMapper.saveAll(clues);
    }

    /// 获取剧本线索
    @Override
    public List<ScriptCluePO> getScriptClues(Long scriptId) {
        return clueMapper.findByScriptId(scriptId);
    }

    /// 获取剧本线索
    @Override
    public List<ScriptCluePO> getScriptCluesByAct(Long scriptId, Integer actOrder) {
        return clueMapper.findByScriptIdAndActOrder(scriptId, actOrder);
    }

    /// 保存剧本提示
    @Override
    @Transactional
    public void saveScriptHints(Long scriptId, List<ScriptHintPO> hints) {
        hints.forEach(hint -> hint.setScriptId(scriptId));
        hintMapper.saveAll(hints);
    }

    /// 获取剧本提示
    @Override
    public List<ScriptHintPO> getScriptHints(Long scriptId) {
        return hintMapper.findByScriptId(scriptId);
    }

    /// 获取剧本提示
    @Override
    public List<ScriptHintPO> getScriptHintsByLevel(Long scriptId, Byte level) {
        return hintMapper.findByScriptIdAndHintLevel(scriptId, level);
    }

    /// 保存剧本评论
    @Override
    @Transactional
    public void saveScriptReview(Long scriptId, ScriptReviewPO review) {
        review.setScriptId(scriptId);
        reviewMapper.save(review);
    }

    /// 获取剧本评论
    @Override
    public ScriptReviewPO getScriptReview(Long scriptId) {
        return reviewMapper.findByScriptId(scriptId).orElse(null);
    }
}
