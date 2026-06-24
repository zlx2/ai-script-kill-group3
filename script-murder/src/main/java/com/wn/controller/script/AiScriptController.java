package com.wn.controller.script;

import com.wn.entity.R;
import com.wn.entity.script.AiScriptTaskPO;
import com.wn.entity.script.ScriptGenRequest;
import com.wn.service.script.AiScriptGenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: 杜江
 * @Description:AI剧本杀控制器类
 * @DateTime: 2026/6/24 11:26
 * @Component:
 **/
@RestController
@RequestMapping("/AiScript")
@RequiredArgsConstructor
@Slf4j
public class AiScriptController {

    @Autowired
    private AiScriptGenService aiScriptGenService;

    /**
     * AI生成剧本
     */
    @PostMapping("/ai/generate")
    public R generate(@RequestBody ScriptGenRequest request) {
        Long taskId = aiScriptGenService.submitTask(request, 1L);
        return new R(taskId);
    }

    /**
     * 查询剧本生成任务状态
     */
    @GetMapping("/ai/task/{taskId}")
    public R getTaskStatus(@PathVariable Long taskId) {
        AiScriptTaskPO task = aiScriptGenService.getTaskStatus(taskId);
        return new R(task);
    }
}