/**
 * @Author: 弗
 * @Description: 
 * @DateTime: 2026/6/25 11:24
 * @Component: 
 **/
package com.wn.service.impl.script;

import com.wn.entity.R;
import com.wn.entity.script.*;
import com.wn.entity.script.stage.Dto.RoleStageDTO;
import com.wn.entity.script.stage.RoomRoleScriptReq;
import com.wn.entity.script.stage.RoomUserRolePO;
import com.wn.entity.script.stage.vo.RolePrivateInfoVO;
import com.wn.entity.script.stage.vo.RoleScriptVO;
import com.wn.mapper.script.RoleStageContentMapper;
import com.wn.mapper.script.RoomUserRoleMapper;
import com.wn.mapper.script.ScriptMapper;
import com.wn.mapper.script.ScriptRoleMapper;
import com.wn.service.script.RoomScriptService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RoomScriptServiceImpl implements RoomScriptService {

    @Resource
    private RoomUserRoleMapper roomUserRoleMapper;
    @Resource
    private ScriptMapper scriptMapper;
    @Resource
    private ScriptRoleMapper scriptRoleMapper;
    @Resource
    private RoleStageContentMapper stageContentMapper;

    @Override
    public R getRoomUserScript(RoomRoleScriptReq req) {
        // 1. 根据roomId+userId 查询本局分配的剧本、角色ID
        Optional<RoomUserRolePO> roomUserOpt = roomUserRoleMapper.findByRoomIdAndUserId(req.getRoomId(), req.getUserId());
        if (roomUserOpt.isEmpty()) {
            return new R(500, "当前用户未分配房间角色");
        }
        RoomUserRolePO roomUser = roomUserOpt.get();
        Long scriptId = roomUser.getScriptId();
        Long roleId = roomUser.getRoleId();

        // 2. 查询剧本基础信息
        ScriptPO script = scriptMapper.findOneByScriptId(scriptId);
        if (script == null) {
            return new R(500, "剧本不存在");
        }

        // 3. 查询角色基础信息（角色名、秘密）
        Optional<ScriptRolePO> roleOpt = scriptRoleMapper.findById(roleId);
        if (roleOpt.isEmpty()) {
            return new R(500, "角色信息不存在");
        }
        ScriptRolePO role = roleOpt.get();

        // 4. 查询该角色所有分幕剧情
        List<RoleStageDTO> chapterList = stageContentMapper.listRoleStage(scriptId, roleId);
        if (chapterList.isEmpty()) {
            return new R(500, "该角色暂无分幕剧本数据");
        }

        // 5. 组装私人隐藏信息
        List<RolePrivateInfoVO> privateInfoList = new ArrayList<>();
        RolePrivateInfoVO secret = new RolePrivateInfoVO();
        secret.setLabel("🔒 私人信息（不可主动暴露）");
        secret.setContent(role.getSecretInfo());
        secret.setUnlockStage("reading");
        secret.setIsUnlocked(true);
        privateInfoList.add(secret);

        // 6. 组装返回VO，前端直接渲染，无假数据
        RoleScriptVO vo = new RoleScriptVO();
        vo.setTitle(script.getScriptName());
        vo.setRoleName(role.getRoleName());
        vo.setBackground(chapterList.get(0).getMainContent());
        vo.setChapters(chapterList);
        vo.setPrivateInfo(privateInfoList);
        // 自我介绍、证据扩展字段，本次先空，后续加表再填充
        vo.setSelfIntro("");
        vo.setEvidenceList(new ArrayList<>());

        return new R(vo);
    }
}
