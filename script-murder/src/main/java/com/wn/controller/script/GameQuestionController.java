/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 17:47
 * @Component:
 **/
package com.wn.controller.script;

import com.wn.entity.R;

import com.wn.entity.script.questions.dto.QuestionAddReq;
import com.wn.entity.script.questions.vo.AnswerSubmitReq;
import com.wn.service.script.GameQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/game/question")
@RequiredArgsConstructor
@Slf4j
public class GameQuestionController {

    private final GameQuestionService gameQuestionService;

    //加载答题题目列表
    @GetMapping("/role/{roleType}")
    public R getQuestionByRole(@PathVariable String roleType) {
        return gameQuestionService.getQuestionByRole(roleType);
    }
    //一次性完成题目 + 选项 + 解析保存
    @PostMapping("/add")
    public R addQuestion(@Valid @RequestBody QuestionAddReq req) {
        log.info("新增题目参数:{}", req);
        return gameQuestionService.addQuestion(req);
    }
    //提交答案判分看解析
    @PostMapping("/answer/submit")
    public R submitAnswer(@Valid @RequestBody AnswerSubmitReq req) {
        return gameQuestionService.submitAnswer(req);
    }

    // 后台新增扩展接口
    @GetMapping("/detail/{id}")
    public R getQuestionDetail(@PathVariable Long id) {
        return gameQuestionService.getQuestionDetail(id);
    }
    //一次性完成题目 + 选项 + 解析编辑题目
    @PutMapping("/edit/{id}")
    public R editQuestion(@PathVariable Long id, @Valid @RequestBody QuestionAddReq req) {
        return gameQuestionService.editQuestion(id, req);
    }

    @DeleteMapping("/{id}")
    public R deleteQuestion(@PathVariable Long id) {
        return gameQuestionService.deleteQuestion(id);
    }

    @GetMapping("/all")
    public R listAllQuestion() {
        return gameQuestionService.listAllQuestion();
    }
}
