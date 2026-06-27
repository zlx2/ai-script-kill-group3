/**
 * @Author: 弗
 * @Description:剧本分幕基本信息添加参数
 * @DateTime: 2026/6/25 14:40
 * @Component:
 **/
package com.wn.entity.script.stage.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScriptStageAddReq {
    @NotNull(message = "剧本ID不能为空")
    private Long scriptId;
    @NotNull(message = "分幕序号stageNo不能为空")
    private Integer stageNo;
    @NotBlank(message = "分幕名称stageName不能为空")
    private String stageName;
    @NotNull(message = "分幕阶段不能为空")
    private String unlockStage;
}
