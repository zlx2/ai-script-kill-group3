/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 17:40
 * @Component:
 **/
package com.wn.entity.script.questions.vo;

import lombok.Data;

// 答题得分返回
@Data
public class AnswerResultVO {
    private Integer totalScore;
    private String analysis; // 答对才返回解析
}
