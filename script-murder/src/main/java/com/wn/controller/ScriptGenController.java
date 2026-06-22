package com.wn.controller;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:34
 * @Component:
 **/

import com.wn.dto.ScriptGenRequest;
import com.wn.entity.AiScriptTask;
import com.wn.entity.R;
import com.wn.service.ScriptGenService;

import org.springframework.web.bind.annotation.*;

/**
 * 剧本生成接口
 */
@RestController
@RequestMapping("/api/ai/script")
public class ScriptGenController {

    private final ScriptGenService scriptGenService;

    public ScriptGenController(ScriptGenService scriptGenService) {
        this.scriptGenService = scriptGenService;
    }

    /**
     * 提交剧本生成任务
     */
    @PostMapping("/generate")
    public R generateScript(@RequestBody ScriptGenRequest request,
                            @RequestHeader("userId") Long userId) {
        Long taskId = scriptGenService.submitTask(request, userId);
        return new R(taskId);
    }

    /**
     * 查询生成任务状态
     */
    @GetMapping("/task/{taskId}")
    public R getTaskStatus(@PathVariable Long taskId) {
        AiScriptTask task = scriptGenService.getTaskStatus(taskId);
        return new R(task);
    }
}
