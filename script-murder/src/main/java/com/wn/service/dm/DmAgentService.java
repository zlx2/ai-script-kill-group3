/**
 * @Author: 杜江
 * @Description: 主持人智能体服务
 * @DateTime: 2026/6/25 14:55
 * @Component:
 **/
package com.wn.service.dm;

public interface DmAgentService {

    void initAgent(String roomId, Long scriptId);

    String analyzeGame(String roomId, String gameContext);

    String executeDecision(String roomId, String decisionJson);

    String autoRun(String roomId);

    String startGame(String roomId, Long scriptId);
}
