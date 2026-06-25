/**
 * @Author: 杜江
 * @Description: 阶段推进管理实现类
 * @DateTime: 2026/6/25 12:07
 * @Component:
 **/
package com.wn.service.impl.dm;

import com.wn.entity.dm.DmRoomStatePO;
import com.wn.mapper.dm.DmRoomStateMapper;
import com.wn.service.dm.DmActService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class DmActServiceImpl implements DmActService {

    private final DmRoomStateMapper roomStateMapper;


    @Override
    @Transactional
    public int advanceAct(String roomId) {
        DmRoomStatePO state = roomStateMapper.findByRoomId(roomId).orElse(null);
        if (state == null) return 0;

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
}
