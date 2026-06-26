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
        // 1. 根据房间+用户 查询本局分配剧本角色
        Optional<RoomUserRolePO> roomUserOpt = roomUserRoleMapper.findByRoomIdAndUserId(req.getRoomId(), req.getUserId());
        if (roomUserOpt.isEmpty()) {
            return new R(500, "当前用户未分配房间角色");
        }
        RoomUserRolePO roomUser = roomUserOpt.get();
        Long scriptId = roomUser.getScriptId();
        Long roleId = roomUser.getRoleId();

        // 2. 查询剧本基础信息
        ScriptPO script = scriptMapper.findById(scriptId).orElse(null);
        if (script == null) {
            return new R(500, "剧本不存在");
        }

        // 3. 查询角色信息（角色名、私人秘密）
        ScriptRolePO role = scriptRoleMapper.findById(roleId).orElse(null);
        if (role == null) {
            return new R(500, "角色信息不存在");
        }

        // 4. 查询该角色全部分幕剧情（关联分幕表拿stageNo、stageName）
        List<RoleStageDTO> stageDtoList = stageContentMapper.listRoleStage(scriptId, roleId);
        if (stageDtoList.isEmpty()) {
            return new R(500, "暂无该角色分幕剧情数据");
        }

        // 5. 组装VO，严格匹配截图返回JSON结构
        RoleScriptVO vo = new RoleScriptVO();
        vo.setTitle(script.getScriptName());
        vo.setRoleName(role.getRoleName());
        // 背景取第一章主线内容
        vo.setBackground(stageDtoList.get(0).getMainContent());

        // 封装chapters数组
        List<RoleScriptVO.ChapterVO> chapterList = new ArrayList<>();
        for (RoleStageDTO dto : stageDtoList) {
            RoleScriptVO.ChapterVO chapter = new RoleScriptVO.ChapterVO();
            chapter.setChapter(dto.getStageNo());
            chapter.setTitle(dto.getStageName());
            chapter.setContent(dto.getMainContent());
            chapter.setUnlockStage(dto.getUnlockStage());
            chapterList.add(chapter);
        }
        vo.setChapters(chapterList);

        // 封装privateInfo数组
        List<RoleScriptVO.PrivateInfoVO> secretList = new ArrayList<>();
        RoleScriptVO.PrivateInfoVO secret = new RoleScriptVO.PrivateInfoVO();
        secret.setLabel("🔒 私人信息（不可主动暴露）");
        secret.setContent(role.getSecretInfo());
        secret.setUnlockStage("reading");
        secretList.add(secret);
        vo.setPrivateInfo(secretList);

        // 返回标准格式：{"code":200,"msg":"执行成功","data":vo}
        return new R(vo);
    }
}
