/**
 * @Author: 杜江
 * @Description: 阶段推进管理
 * @DateTime: 2026/6/25 12:06
 * @Component:
 **/
package com.wn.service.dm;

public interface DmActService {

    /**
     * 推进阶段
     */
    int advanceAct(String roomId);

    /**
     * 获取当前阶段（从 DmRoomStatePO 中获取）
     */
    int getCurrentAct(String roomId);

    /**
     * 回滚阶段
     */
    void rollbackAct(String roomId);
}
