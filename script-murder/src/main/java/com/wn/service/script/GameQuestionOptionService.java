package com.wn.service.script;

import com.wn.entity.R;
import com.wn.entity.script.questions.dto.GameOptionEditDTO;
import java.util.List;

public interface GameQuestionOptionService {
    // 批量新增选项
    R batchAddOption(List<GameOptionEditDTO> dtoList);
    // 修改单条选项
    R editOption(GameOptionEditDTO dto);
    // 删除单条选项
    R deleteOption(Long optionId);
    // 根据题目id查询全部选项
    R listOptionByQuestionId(Long questionId);
    // 根据id获取单条选项
    R getOptionById(Long optionId);
}
