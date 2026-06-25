/**
 * @Author: 弗
 * @Description:
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

    @PostMapping("/save")
    public R saveAnalysis(@Valid @RequestBody GameAnalysisEditDTO dto) {
        return analysisService.saveAnalysis(dto);
    }

    @GetMapping("/{questionId}")
    public R getAnalysisByQuestionId(@PathVariable Long questionId) {
        return analysisService.getAnalysisByQuestionId(questionId);
    }

    @DeleteMapping("/{questionId}")
    public R deleteAnalysis(@PathVariable Long questionId) {
        return analysisService.deleteAnalysis(questionId);
    }
}
