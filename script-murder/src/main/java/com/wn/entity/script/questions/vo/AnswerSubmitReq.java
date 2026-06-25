/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 17:40
 * @Component:
 **/
package com.wn.entity.script.questions.vo;

import lombok.Data;

import java.util.List;

// 答题提交入参
@Data
public class AnswerSubmitReq {
    private Long questionId;
    private List<String> selectCodes; // 用户选择的A/B/C
}
