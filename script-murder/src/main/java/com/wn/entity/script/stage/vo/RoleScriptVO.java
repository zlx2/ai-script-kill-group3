/**
 * @Author: 弗
 * @Description: 
 * @DateTime: 2026/6/25 11:12
 * @Component: 
 **/
package com.wn.entity.script.stage.vo;

import com.wn.entity.script.stage.Dto.RoleStageDTO;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RoleScriptVO {
    private String title;
    private String roleName;
    private String background;
    private List<RoleStageDTO> chapters = new ArrayList<>();
    private List<RolePrivateInfoVO> privateInfo = new ArrayList<>();
    private String selfIntro;
    private List<String> evidenceList = new ArrayList<>();
}
