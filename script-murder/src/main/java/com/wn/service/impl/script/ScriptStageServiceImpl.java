/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 14:41
 * @Component:
 **/
package com.wn.service.impl.script;

import com.wn.entity.R;
import com.wn.entity.script.ScriptRolePO;
import com.wn.entity.script.stage.RoleStageContentPO;
import com.wn.entity.script.stage.RoomUserRolePO;
import com.wn.entity.script.stage.ScriptStagePO;
import com.wn.entity.script.stage.req.RoleStageContentAddReq;
import com.wn.entity.script.stage.req.RoomUserRoleAddReq;
import com.wn.entity.script.stage.req.ScriptRoleAddReq;
import com.wn.entity.script.stage.req.ScriptStageAddReq;
import com.wn.mapper.script.RoleStageContentMapper;
import com.wn.mapper.script.RoomUserRoleMapper;
import com.wn.mapper.script.ScriptRoleMapper;
import com.wn.mapper.script.ScriptStageMapper;
import com.wn.service.script.ScriptStageService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScriptStageServiceImpl implements ScriptStageService {
    @Resource
    private RoleStageContentMapper roleStageContentMapper;
    @Resource
    private RoomUserRoleMapper roomUserRoleMapper;
    @Resource
    private ScriptStageMapper scriptStageMapper;
    @Resource
    private ScriptRoleMapper scriptRoleMapper;

    @Override
    public R addScriptRole(ScriptRoleAddReq req) {
        ScriptRolePO po = new ScriptRolePO();
        //req转po
        BeanUtils.copyProperties(req, po);
        ScriptRolePO save = scriptRoleMapper.save(po);
        return new R(save);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R addRoleStageContent(RoleStageContentAddReq req) {
        RoleStageContentPO po = new RoleStageContentPO();
        BeanUtils.copyProperties(req, po);
        RoleStageContentPO save = roleStageContentMapper.save(po);
        return new R(save);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R addRoomUserRole(RoomUserRoleAddReq req) {
        RoomUserRolePO po = new RoomUserRolePO();
        BeanUtils.copyProperties(req, po);
        RoomUserRolePO save = roomUserRoleMapper.save(po);
        return new R(save);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R addScriptStage(ScriptStageAddReq req) {
        ScriptStagePO po = new ScriptStagePO();
        BeanUtils.copyProperties(req, po);
        ScriptStagePO save = scriptStageMapper.save(po);
        return new R(save);
    }
}
