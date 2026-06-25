package com.wn.service.impl.clue;

import com.wn.entity.clue.ScriptCluePO;
import com.wn.entity.dm.RoomCluePO;
import com.wn.entity.script.ScriptPO;
import com.wn.entity.script.ScriptRolePO;
import com.wn.mapper.clue.ClueMapper;
import com.wn.mapper.dm.DmRoomClueMapper;
import com.wn.mapper.script.ScriptMapper;
import com.wn.mapper.script.ScriptRoleMapper;
import com.wn.service.clue.ClueService;
import com.wn.utils.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 线索服务实现类
 * 主要是依靠中间表来实现同一个房间内角色的线索公开状态同步
 * 具体表：dm_room_clue
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClueServiceImpl implements ClueService {

    private final ClueMapper clueMapper;
    private final ScriptRoleMapper scriptRoleMapper;
    private final DmRoomClueMapper dmRoomClueMapper;
    private final ScriptMapper scriptMapper;

    /**
     * 获取所有线索
     *  当前角色可以获取他的私人线索以及公开线索
     *  逻辑：
     *      1. 检查角色是否存在
     *      2. 检查角色是否属于当前剧本
     *      3. 查询该剧本下所有 <= 当前场景的线索
     *      4. 查询该房间已公开的线索ID
     *      5. 过滤：公开线索 + 自己的私人线索 + 已揭示的线索
     *      6. 返回过滤后的线索列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<ScriptCluePO> getAllClues(Long roleId, int scene, String roomId) {
        // 1. 参数校验
        if (roleId == null || scene < 0) {
            throw new GlobalException(400, "角色ID或场景不合规");
        }

        // 2. 查询角色
        ScriptRolePO role = scriptRoleMapper.findById(roleId)
                .orElseThrow(() -> new GlobalException(404, "角色不存在"));

        // 3. 查询该剧本下所有 <= 当前场景的线索
        List<ScriptCluePO> all = clueMapper.findByScriptIdAndSceneLessThanEqual(
                role.getScriptId(), scene);

        // 4. 查询该房间已公开的线索ID
        List<RoomCluePO> roomClues = dmRoomClueMapper.findByRoomId(roomId);
        if (roomClues == null) {
            throw new GlobalException(404, "房间不存在");
        }
        Set<Long> revealedIds = roomClues.stream()
                .filter(rc -> rc.getIsPublic() == 1)// 只取公开的线索
                .map(RoomCluePO::getClueId)
                .collect(Collectors.toSet());

        // 5. 过滤：公开线索 + 自己的私人线索 + 已揭示的线索
        List<ScriptCluePO> result = all.stream()
                .filter(c -> c.getIsPublic() == 1
                        || (c.getRoleId() != null && c.getRoleId().equals(roleId))
                        || revealedIds.contains(c.getClueId()))
                .collect(Collectors.toList());

        log.info("获取线索列表 roleId={}, scene={}, roomId={}, 结果数={}",
                roleId, scene, roomId, result.size());

        return result;
    }

    /**
     * 公开线索
     *  公开线索逻辑：
     *      1. 检查线索是否存在
     *      2. 检查角色是否存在
     *      3. 检查角色是否属于当前剧本
     *      4. 检查线索是否已公开
     *      5. 如果未公开，新增公开记录
     *      6. 返回更新后的线索列表
     */
    @Override
    @Transactional  // 加上事务
    public List<ScriptCluePO> openClue(String roomId, Long clueId, Long userId, Long roleId, int scene) {
        // 1. 查询线索
        ScriptCluePO clue = clueMapper.findById(clueId)
                .orElseThrow(() -> new GlobalException(404, "线索不存在"));

        // 2. 查询角色
        ScriptRolePO role = scriptRoleMapper.findById(roleId)
                .orElseThrow(() -> new GlobalException(404, "角色不存在"));

        // 3. 权限校验：线索必须属于当前剧本
        if (!clue.getScriptId().equals(role.getScriptId())) {
            throw new GlobalException(403, "该线索不属于当前剧本，无法公开");
        }

        // 4. 如果已经公开，直接返回
        if (clue.getIsPublic() == 1) {
            return getAllClues(roleId, scene, roomId);
        }

        // 5. 如果该房间还没有这条线索的公开记录，新增
        if (!dmRoomClueMapper.existsByRoomIdAndClueId(roomId, clueId)) {
            RoomCluePO rc = new RoomCluePO();
            rc.setRoomId(roomId);
            rc.setPlayerId(userId);
            rc.setClueId(clueId);
            rc.setIsPublic((byte) 1);
            dmRoomClueMapper.save(rc);
            log.info("线索公开成功 roomId={}, clueId={}, userId={}", roomId, clueId, userId);
        }

        // 6. 返回更新后的线索列表
        return getAllClues(roleId, scene, roomId);
    }

    /**
     * 新增角色线索类
     */
    @Override
    public void addClue(ScriptCluePO scriptCluePO) {
        // 1. 校验角色是否存在
        ScriptRolePO role = scriptRoleMapper.findById(scriptCluePO.getRoleId())
                .orElseThrow(() -> new GlobalException(404, "角色不存在"));
        // 2. 校验剧本是否存在
        ScriptPO script = scriptMapper.findById(role.getScriptId())
                .orElseThrow(() -> new GlobalException(404, "剧本不存在"));

        clueMapper.save(scriptCluePO);
    }
}
