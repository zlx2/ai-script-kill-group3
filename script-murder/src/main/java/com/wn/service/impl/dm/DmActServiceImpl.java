/**
 * @Author: 杜江
 * @Description: 阶段推进管理实现类
 * @DateTime: 2026/6/25 12:07
 * @Component:
 **/
package com.wn.service.impl.dm;

import com.wn.entity.dm.DmRoomStatePO;
import com.wn.entity.script.stage.ScriptStagePO;
import com.wn.mapper.dm.DmRoomStateMapper;
import com.wn.mapper.script.ScriptStageMapper;
import com.wn.service.dm.DmActService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class DmActServiceImpl implements DmActService {

    // 注入队友的 Mapper
    private final ScriptStageMapper stageMapper;

    private final DmRoomStateMapper roomStateMapper;

    @Override
    @Transactional
    public int advanceAct(String roomId) {
        DmRoomStatePO state = roomStateMapper.findByRoomId(roomId).orElse(null);
        if (state == null) return 0;

        // 获取剧本的总阶段数
        Long scriptId = getScriptIdByRoomId(roomId);
        List<ScriptStagePO> stages = stageMapper.findByScriptIdOrderByStageNoAsc(scriptId);

        if (state.getCurrentAct() >= stages.size()) {
            return state.getCurrentAct();
        }

        state.setCurrentAct(state.getCurrentAct() + 1);
        roomStateMapper.save(state);
        return state.getCurrentAct();
    }

    @Override
    public int getCurrentAct(String roomId) {
        DmRoomStatePO state = roomStateMapper.findByRoomId(roomId).orElse(null);
        return state != null ? state.getCurrentAct() : 1;
    }

    @Override
    @Transactional
    public void rollbackAct(String roomId) {
        DmRoomStatePO state = roomStateMapper.findByRoomId(roomId).orElse(null);
        if (state != null && state.getCurrentAct() > 1) {
            state.setCurrentAct(state.getCurrentAct() - 1);
            roomStateMapper.save(state);
        }
    }

    private Long getScriptIdByRoomId(String roomId) {
        DmRoomStatePO state = roomStateMapper.findByRoomId(roomId).orElse(null);
        return state != null ? state.getScriptId() : 1L;
    }
}
