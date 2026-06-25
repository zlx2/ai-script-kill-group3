/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 18:46
 * @Component:
 **/
package com.wn.service.script;

import com.wn.entity.R;
import com.wn.entity.script.questions.dto.GameAnalysisEditDTO;

public interface GameQuestionAnalysisService {
    // 新增/修改解析
    R saveAnalysis(GameAnalysisEditDTO dto);

    // 根据题目id查询解析
    R getAnalysisByQuestionId(Long questionId);

    // 删除解析
    R deleteAnalysis(Long questionId);
}
