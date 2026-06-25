/**
 * @Author: 弗
 * @Description: 
 * @DateTime: 2026/6/25 11:10
 * @Component: 
 **/
package com.wn.entity.script.stage.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleStageDTO {
    private Integer stageNo;
    private String stageName;
    private String mainContent;
    private String hintContent;
    private String unlockStage;
    private Boolean isUnlocked;
}
