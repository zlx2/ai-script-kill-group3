/**
 * @Author: 杜江
 * @Description: 阶段推进管理
 * @DateTime: 2026/6/25 12:06
 * @Component:
 **/
package com.wn.service.dm;

public interface DmActService {

    int advanceAct(String roomId);
    int getCurrentAct(String roomId);
    void rollbackAct(String roomId);
}
