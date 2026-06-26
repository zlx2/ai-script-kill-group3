/**
 * @Author: 杜江
 * @Description: dm房间服务实现类
 * @DateTime: 2026/6/25 12:05
 * @Component:
 **/
package com.wn.service.impl.dm;

import com.wn.entity.dm.DmRoomStatePO;
import com.wn.mapper.dm.DmRoomStateMapper;
import com.wn.service.dm.DmRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DmRoomServiceImpl implements DmRoomService {

    private final DmRoomStateMapper roomStateMapper;

    @Override
    @Transactional
    public DmRoomStatePO initRoomState(String roomId, Long scriptId) {
        DmRoomStatePO state = new DmRoomStatePO();
        state.setRoomId(roomId);
        state.setScriptId(scriptId);
        state.setCurrentAct(1);
        return roomStateMapper.save(state);
    }

    @Override
    public DmRoomStatePO getRoomState(String roomId) {
        return roomStateMapper.findByRoomId(roomId).orElse(null);
    }

    @Override
    @Transactional
    public void updateRoomSettings(String roomId, Map<String, Object> settings) {
        DmRoomStatePO state = roomStateMapper.findByRoomId(roomId).orElse(null);
        if (state == null) return;

        if (settings.containsKey("isPrivateChatEnabled")) {
            state.setIsPrivateChatEnabled((Byte) settings.get("isPrivateChatEnabled"));
        }
        if (settings.containsKey("isAiTalkEnabled")) {
            state.setIsAiTalkEnabled((Byte) settings.get("isAiTalkEnabled"));
        }
        if (settings.containsKey("searchRoundCount")) {
            state.setSearchRoundCount((Integer) settings.get("searchRoundCount"));
        }
        if (settings.containsKey("chatDurationMinutes")) {
            state.setChatDurationMinutes((Integer) settings.get("chatDurationMinutes"));
        }

        roomStateMapper.save(state);
    }

    @Override
    @Transactional
    public void enablePrivateChat(String roomId) {
        updateSetting(roomId, "isPrivateChatEnabled", (byte) 1);
    }

    @Override
    @Transactional
    public void disablePrivateChat(String roomId) {
        updateSetting(roomId, "isPrivateChatEnabled", (byte) 0);
    }

    @Override
    @Transactional
    public void enableAiTalk(String roomId) {
        updateSetting(roomId, "isAiTalkEnabled", (byte) 1);
    }

    @Override
    @Transactional
    public void disableAiTalk(String roomId) {
        updateSetting(roomId, "isAiTalkEnabled", (byte) 0);
    }

    @Override
    @Transactional
    public void setSearchRoundCount(String roomId, Integer count) {
        updateSetting(roomId, "searchRoundCount", count);
    }

    @Override
    @Transactional
    public void setChatDuration(String roomId, Integer minutes) {
        updateSetting(roomId, "chatDurationMinutes", minutes);
    }

    private void updateSetting(String roomId, String field, Object value) {
        DmRoomStatePO state = roomStateMapper.findByRoomId(roomId).orElse(null);
        if (state == null) return;

        try {
            java.lang.reflect.Field fieldObj = DmRoomStatePO.class.getDeclaredField(field);
            fieldObj.setAccessible(true);
            fieldObj.set(state, value);
            roomStateMapper.save(state);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
