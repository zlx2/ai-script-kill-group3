/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 18:52
 * @Component:
 **/
package com.wn.controller.script;
import com.wn.entity.R;
import com.wn.entity.script.questions.dto.GameOptionEditDTO;

import com.wn.service.script.GameQuestionOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/game/option")
@RequiredArgsConstructor
public class GameQuestionOptionController {
    private final GameQuestionOptionService optionService;

    @PostMapping("/batch")//测试通过
    public R batchAddOption(@Valid @RequestBody List<GameOptionEditDTO> dtoList) {
        return optionService.batchAddOption(dtoList);
    }

    @PutMapping("/edit")//测试通过
    public R editOption(@Valid @RequestBody GameOptionEditDTO dto) {
        return optionService.editOption(dto);
    }

    @DeleteMapping("/{optionId}")//测试通过
    public R deleteOption(@PathVariable Long optionId) {
        return optionService.deleteOption(optionId);
    }

    @GetMapping("/list/{questionId}")//测试通过
    public R listOptionByQuestionId(@PathVariable Long questionId) {
        return optionService.listOptionByQuestionId(questionId);
    }

    @GetMapping("/{optionId}")//测试通过
    public R getOptionById(@PathVariable Long optionId) {
        return optionService.getOptionById(optionId);
    }
}
