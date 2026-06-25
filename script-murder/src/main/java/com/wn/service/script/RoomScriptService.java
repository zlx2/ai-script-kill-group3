/**
 * @Author: 弗
 * @Description: 
 * @DateTime: 2026/6/25 11:23
 * @Component: 
 **/
package com.wn.service.script;

import com.wn.entity.R;
import com.wn.entity.script.stage.RoomRoleScriptReq;


public interface RoomScriptService {
    R getRoomUserScript(RoomRoleScriptReq req);
}
