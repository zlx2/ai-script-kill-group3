package com.wn.service.script;

import com.wn.entity.script.AiScriptTaskPO;
import com.wn.entity.script.ScriptGenRequest;

/**
 * @Author: 杜江
 * @Description:AI剧本杀服务类，负责调用AgentScope生成剧本
 * @DateTime: 2026/6/24 11:00
 * @Component:
 **/

public interface AiScriptGenService {
    Long submitTask(ScriptGenRequest request, Long userId);
    AiScriptTaskPO getTaskStatus(Long taskId);
}
