/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 18:50
 * @Component:
 **/
package com.wn.service.impl.script;

import com.wn.entity.R;

import com.wn.entity.script.questions.GameQuestionAnalysisPO;
import com.wn.entity.script.questions.dto.GameAnalysisEditDTO;

import com.wn.mapper.script.GameQuestionAnalysisRepository;
import com.wn.service.script.GameQuestionAnalysisService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GameQuestionAnalysisServiceImpl implements GameQuestionAnalysisService {

    @Resource
    private GameQuestionAnalysisRepository analysisRepo;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R saveAnalysis(GameAnalysisEditDTO dto) {
        List<GameQuestionAnalysisPO> oldList = analysisRepo.findByQuestionId(dto.getQuestionId());
        GameQuestionAnalysisPO po;
        if (!oldList.isEmpty()) {
            // 取第一条旧解析更新
            po = oldList.get(0);
        } else {
            po = new GameQuestionAnalysisPO();
        }
        BeanUtils.copyProperties(dto, po);
        po.setQuestionId(dto.getQuestionId());
        GameQuestionAnalysisPO save = analysisRepo.save(po);
        return R.success(save);
    }

    @Override
    public R getAnalysisByQuestionId(Long questionId) {
        List<GameQuestionAnalysisPO> list = analysisRepo.findByQuestionId(questionId);
        if (list.isEmpty()) {
            return R.error("解析不存在");
        }
        // 取第一条返回
        return R.success(list.get(0));
    }

    @Override
    @Transactional
    public R deleteAnalysis(Long questionId) {
        analysisRepo.deleteByQuestionId(questionId);
        return R.success();
    }
}
