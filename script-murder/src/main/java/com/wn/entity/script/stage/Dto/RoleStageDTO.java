/**
 * @Author: 弗
 * @Description: 查询角色分幕剧情DTO
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
    //分幕序号
    private Integer stageNo;
    //分幕标题
    private String stageName;
    //分幕内容
    private String mainContent;
    //角色分幕隐藏信息
    private String hintContent;
    //解锁分幕阶段
    private String unlockStage;
    //是否解锁
    private Boolean isUnlocked;
}
