/**
 * @Author: 弗
 * @Description:问题查询，提交返回答案解析，新增，修改，删除，题目详情
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

    // 玩家查询：路径携带剧本ID、角色ID
    @GetMapping("/{scriptId}/{roleId}")
    public R getQuestionByRole(
            @PathVariable Long scriptId,
            @PathVariable Long roleId
    ) {
        return gameQuestionService.getQuestionByRole(scriptId, roleId);
    }

    // 后台新增
    @PostMapping("/add")
    public R addQuestion(@Valid @RequestBody QuestionAddReq req) {
        log.info("新增题目参数:{}", req);
        return gameQuestionService.addQuestion(req);
    }

    // 答题提交，路径携带剧本、角色ID做校验
    @PostMapping("/answer/submit/{scriptId}/{roleId}")
    public R submitAnswer(
            @Valid @RequestBody AnswerSubmitReq req,
            @PathVariable Long scriptId,
            @PathVariable Long roleId
    ) {
        return gameQuestionService.submitAnswer(req, scriptId, roleId);
    }

    // 后台根据剧本查所有题目
    @GetMapping("/script/{scriptId}/all")
    public R listAllQuestionByScript(@PathVariable Long scriptId) {
        return gameQuestionService.listAllQuestionByScript(scriptId);
    }
    //获取题目详情
    @GetMapping("/detail/{id}")
    public R getQuestionDetail(@PathVariable Long id) {
        return gameQuestionService.getQuestionDetail(id);
    }
    //编辑题目
    @PutMapping("/edit/{id}")
    public R editQuestion(@PathVariable Long id, @Valid @RequestBody QuestionAddReq req) {
        return gameQuestionService.editQuestion(id, req);
    }
    //删除题目
    @DeleteMapping("/{id}")
    public R deleteQuestion(@PathVariable Long id) {
        return gameQuestionService.deleteQuestion(id);
    }
}
