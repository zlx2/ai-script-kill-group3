/**
 * @Author: 杜江
 * @Description: dm房间服务接口
 * @DateTime: 2026/6/25 12:05
 * @Component:
 **/
package com.wn.service.dm;



import com.wn.entity.dm.DmRoomStatePO;

import java.util.Map;

public interface DmRoomService {

    DmRoomStatePO initRoomState(String roomId, Long scriptId);
    DmRoomStatePO getRoomState(String roomId);
    void updateRoomSettings(String roomId, Map<String, Object> settings);

    void enablePrivateChat(String roomId);
    void disablePrivateChat(String roomId);

    void enableAiTalk(String roomId);
    void disableAiTalk(String roomId);

    void setSearchRoundCount(String roomId, Integer count);
    void setChatDuration(String roomId, Integer minutes);
}
