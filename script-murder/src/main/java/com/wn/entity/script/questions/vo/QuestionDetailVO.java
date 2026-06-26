/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/26 14:16
 * @Component:
 **/
package com.wn.entity.script.questions.vo;

import lombok.Data;
import java.util.List;

@Data
public class QuestionDetailVO {
    private Long id;
    private Long scriptId;
    private Long roleId;
    private String questionTitle;
    private Integer sortNum;
    private List<QuestionOptionVO> options;
    private String analysis;
}
