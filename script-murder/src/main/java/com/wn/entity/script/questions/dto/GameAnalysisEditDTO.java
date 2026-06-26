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

// 单独修改解析
@Data
public class GameAnalysisEditDTO {
    private Long id;
    @NotNull
    private Long questionId;
    @NotBlank
    private String analysis;
    @NotNull(message = "所属剧本ID不能为空")
    private Long scriptId;

    @NotNull(message = "所属角色ID不能为空")
    private Long roleId;
}
