/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 18:47
 * @Component:
 **/
package com.wn.service.impl.script;

import com.wn.entity.R;

import com.wn.entity.script.questions.GameQuestionOptionPO;
import com.wn.entity.script.questions.dto.GameOptionEditDTO;

import com.wn.mapper.script.GameQuestionOptionRepository;
import com.wn.service.script.GameQuestionOptionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GameQuestionOptionServiceImpl implements GameQuestionOptionService {

    @Resource
    private GameQuestionOptionRepository optionRepo;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R batchAddOption(List<GameOptionEditDTO> dtoList) {
        List<GameQuestionOptionPO> poList = new ArrayList<>();
        for (int i = 0; i < dtoList.size(); i++) {
            GameOptionEditDTO dto = dtoList.get(i);
            GameQuestionOptionPO po = new GameQuestionOptionPO();
            po.setQuestionId(dto.getQuestionId());
            po.setOptionCode(dto.getOptionCode());
            po.setOptionContent(dto.getOptionContent());
            po.setIsCorrect(dto.getIsCorrect());
            po.setScore(dto.getScore());
            poList.add(po);
        }
        List<GameQuestionOptionPO> saveList = optionRepo.saveAll(poList);
        return R.success(saveList);
    }

    @Override
    public R editOption(GameOptionEditDTO dto) {
        Optional<GameQuestionOptionPO> optional = optionRepo.findById(dto.getId());
        if (optional.isEmpty()) {
            return R.error("选项不存在");
        }
        GameQuestionOptionPO po = optional.get();
        po.setQuestionId(dto.getQuestionId());
        po.setOptionCode(dto.getOptionCode());
        po.setOptionContent(dto.getOptionContent());
        po.setIsCorrect(dto.getIsCorrect());
        po.setScore(dto.getScore());
        GameQuestionOptionPO update = optionRepo.save(po);
        return R.success(update);
    }

    @Override
    public R deleteOption(Long optionId) {
        optionRepo.deleteById(optionId);
        return R.success();
    }

    @Override
    public R listOptionByQuestionId(Long questionId) {
        List<GameQuestionOptionPO> list = optionRepo.findByQuestionId(questionId);
        return R.success(list);
    }

    @Override
    public R getOptionById(Long optionId) {
        Optional<GameQuestionOptionPO> optional = optionRepo.findById(optionId);
        if (optional.isEmpty()) {
            return R.error("选项不存在");
        }
        return R.success(optional.get());
    }
}