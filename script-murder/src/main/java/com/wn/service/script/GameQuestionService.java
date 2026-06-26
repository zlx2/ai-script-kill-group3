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
import com.wn.entity.script.questions.vo.QuestionDetailVO;

public interface GameQuestionService {
    // 玩家端：根据剧本+角色ID查询题目
    R getQuestionByRole(Long scriptId, Long roleId);

    // 后台新增完整题目（携带剧本、角色ID）
    R addQuestion(QuestionAddReq req);

    // 玩家提交答题判分（增加剧本、角色做权限校验）
    R submitAnswer(AnswerSubmitReq req, Long scriptId, Long roleId);

    // 后台：单条详情
    R getQuestionDetail(Long id);

    // 后台：编辑题目
    R editQuestion(Long id, QuestionAddReq req);

    // 后台：删除题目（级联删选项、解析）
    R deleteQuestion(Long id);

    // 后台：根据剧本查询全部题目
    R listAllQuestionByScript(Long scriptId);
}
