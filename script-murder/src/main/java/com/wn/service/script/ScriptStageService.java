/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 14:40
 * @Component:
 **/
package com.wn.service.script;

import com.wn.entity.R;
import com.wn.entity.script.stage.req.RoleStageContentAddReq;
import com.wn.entity.script.stage.req.RoomUserRoleAddReq;
import com.wn.entity.script.stage.req.ScriptRoleAddReq;
import com.wn.entity.script.stage.req.ScriptStageAddReq;
import com.wn.entity.script.stage.RoleStageContentPO;
import com.wn.entity.script.stage.RoomUserRolePO;
import com.wn.entity.script.stage.ScriptStagePO;

public interface ScriptStageService {
    R addScriptRole(ScriptRoleAddReq req);
    // 新增角色分幕剧情
    R addRoleStageContent(RoleStageContentAddReq req);
    // 房间分配用户角色
    R addRoomUserRole(RoomUserRoleAddReq req);
    // 新增剧本分幕阶段
    R addScriptStage(ScriptStageAddReq req);
}
