/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 17:41
 * @Component:
 **/
package com.wn.entity.script.questions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class QuestionAddReq {
    @NotNull(message = "所属剧本ID不能为空")
    private Long scriptId;

    @NotNull(message = "所属角色ID不能为空")
    private Long roleId;

    @NotBlank(message = "题干不能为空")
    private String questionTitle;

    @NotNull(message = "排序号不能为空")
    private Integer sortNum;

    @NotNull(message = "选项列表不能为空")
    private List<QuestionOptionDto> optionList;

    @NotBlank(message = "标准答案解析不能为空")
    private String analysis;

    @Data
    public static class QuestionOptionDto {
        @NotBlank
        private String optionCode;
        @NotBlank
        private String optionContent;
        @NotNull
        private Integer isCorrect;
        @NotNull
        private Integer score;
    }
}
