/**
 * @Author: 弗
 * @Description:问题解析，不是必选添加接口
 * @DateTime: 2026/6/25 18:53
 * @Component:
 **/
package com.wn.controller.script;

import com.wn.entity.R;
import com.wn.entity.script.questions.dto.GameAnalysisEditDTO;

import com.wn.service.script.GameQuestionAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game/analysis")
@RequiredArgsConstructor
public class GameQuestionAnalysisController {

    private final GameQuestionAnalysisService analysisService;
    /**
     * 保存问题解析
     */
    @PostMapping("/save")//测试通过
    public R saveAnalysis(@Valid @RequestBody GameAnalysisEditDTO dto) {
        return analysisService.saveAnalysis(dto);
    }
    //有全部问题信息，这个也是可选
    /**
     * 根据问题ID获取问题解析
     */
    @GetMapping("/{questionId}")//测试通过
    public R getAnalysisByQuestionId(@PathVariable Long questionId) {
        return analysisService.getAnalysisByQuestionId(questionId);
    }
    //有全删方法，这个前段看需选择
    /**
     * 删除问题解析
     */
    @DeleteMapping("/{questionId}")//测试通过，增加了事务管理
    public R deleteAnalysis(@PathVariable Long questionId) {
        return analysisService.deleteAnalysis(questionId);
    }
}
