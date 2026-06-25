/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 17:42
 * @Component:
 **/
package com.wn.service.script;

import com.wn.entity.R;
import com.wn.entity.script.questions.dto.QuestionAddReq;
import com.wn.entity.script.questions.vo.AnswerSubmitReq;

public interface GameQuestionService {
    // 根据角色获取所有题目（前端玩家查询）
    R getQuestionByRole(String roleType);

    // 后台新增题目
    R addQuestion(QuestionAddReq req);

    // 提交答题判分
    R submitAnswer(AnswerSubmitReq req);
    // 后台管理 新增扩展方法
    // 根据id查询完整题目（含答案、解析，管理端）
    R getQuestionDetail(Long id);
    // 修改题目
    R editQuestion(Long id, QuestionAddReq req);
    // 删除题目（级联删除选项、解析）
    R deleteQuestion(Long id);
    // 查询所有题目分页/全量
    R listAllQuestion();
}
