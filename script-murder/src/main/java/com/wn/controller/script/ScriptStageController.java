/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 14:44
 * @Component:
 **/
package com.wn.controller.script;
import com.wn.entity.R;
import com.wn.entity.script.stage.req.RoleStageContentAddReq;
import com.wn.entity.script.stage.req.RoomUserRoleAddReq;
import com.wn.entity.script.stage.req.ScriptRoleAddReq;
import com.wn.entity.script.stage.req.ScriptStageAddReq;
import com.wn.service.script.ScriptStageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/script/stage")
@RequiredArgsConstructor
@Slf4j
public class ScriptStageController {
    private final ScriptStageService scriptStageService;

    /**
     * 新增剧本角色
     * POST /api/script/stage/role/add
     */
    @PostMapping("/role/add")
    public R addScriptRole(@Valid @RequestBody ScriptRoleAddReq req) {
        log.info("新增角色入参：{}", req);
        return scriptStageService.addScriptRole(req);
    }
    /**
     * 新增角色分幕剧情
     */
    @PostMapping("/role-content")
    public R addRoleStageContent(@Valid @RequestBody RoleStageContentAddReq req) {
        log.info("新增角色分幕剧情入参:{}", req);
        return scriptStageService.addRoleStageContent(req);
    }

    /**
     * 房间分配用户角色
     */
    @PostMapping("/room-user-role")
    public R addRoomUserRole(@Valid @RequestBody RoomUserRoleAddReq req) {
        log.info("分配房间角色入参:{}", req);
        return scriptStageService.addRoomUserRole(req);
    }

    /**
     * 新增剧本分幕阶段
     */
    @PostMapping("/stage")
    public R addScriptStage(@Valid @RequestBody ScriptStageAddReq req) {
        log.info("新增剧本阶段入参:{}", req);
        return scriptStageService.addScriptStage(req);
    }
}
