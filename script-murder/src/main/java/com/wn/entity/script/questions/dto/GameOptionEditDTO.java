/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 18:37
 * @Component:
 **/
package com.wn.entity.script.questions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GameOptionEditDTO {
    private Long id;
    @NotNull
    private Long questionId;
    @NotBlank
    private String optionCode;
    @NotBlank
    private String optionContent;
    private Integer isCorrect;
    private Integer score;
}

