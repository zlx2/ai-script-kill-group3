/**
 * @Author: 弗
 * @Description: 
 * @DateTime: 2026/6/25 11:10
 * @Component: 
 **/
package com.wn.entity.script.stage.vo;

import lombok.Data;

@Data
public class RolePrivateInfoVO {
    private String label;
    private String content;
    private String unlockStage;
    private Boolean isUnlocked;
}
