/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 17:37
 * @Component:
 **/
package com.wn.entity.script.questions.vo;

import lombok.Data;
import java.util.List;

@Data
public class QuestionVO {
    private Long questionId;
    private String questionTitle;
    private List<QuestionOptionVO> options;
}




