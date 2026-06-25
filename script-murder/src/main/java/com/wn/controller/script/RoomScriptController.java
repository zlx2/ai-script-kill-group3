/**
 * @Author: 弗
 * @Description: 
 * @DateTime: 2026/6/25 11:24
 * @Component: 
 **/
package com.wn.controller.script;

import com.wn.entity.R;


import com.wn.entity.script.stage.RoomRoleScriptReq;
import com.wn.entity.script.stage.vo.RoleScriptVO;
import com.wn.service.script.RoomScriptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/roomScript")
@RequiredArgsConstructor
@Slf4j
public class RoomScriptController {

    private final RoomScriptService roomScriptService;

    /**
     * 根据房间id、用户id 获取当前玩家真实分幕剧本
     * 前端RoomDetail.vue 调用，无任何假数据，全部数据库读取
     */
    @PostMapping("/getUserScript")
    public R getUserScript(@RequestBody RoomRoleScriptReq req) {
        log.info("获取房间剧本请求：{}", req);
        return roomScriptService.getRoomUserScript(req);
    }
}
