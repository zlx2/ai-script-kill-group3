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

import java.util.Optional;

@Service
public class GameQuestionAnalysisServiceImpl implements GameQuestionAnalysisService {

    @Resource
    private GameQuestionAnalysisRepository analysisRepo;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R saveAnalysis(GameAnalysisEditDTO dto) {
        Optional<GameQuestionAnalysisPO> old = analysisRepo.findByQuestionId(dto.getQuestionId());
        GameQuestionAnalysisPO po;
        if (old.isPresent()) {
            po = old.get();
        } else {
            po = new GameQuestionAnalysisPO();
        }
        BeanUtils.copyProperties(dto, po);
        GameQuestionAnalysisPO save = analysisRepo.save(po);
        return R.success(save);
    }

    @Override
    public R getAnalysisByQuestionId(Long questionId) {
        Optional<GameQuestionAnalysisPO> optional = analysisRepo.findByQuestionId(questionId);
        if (optional.isEmpty()) {
            return R.error("解析不存在");
        }
        return R.success(optional.get());
    }

    @Override
    @Transactional
    public R deleteAnalysis(Long questionId) {
        analysisRepo.deleteByQuestionId(questionId);
        return R.success();
    }
}
