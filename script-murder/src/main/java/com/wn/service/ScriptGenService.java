package com.wn.service;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:30
 * @Component:
 **/

import com.wn.ai.agent.PlotDesignAgent;
import com.wn.ai.agent.RoleDesignAgent;
import com.wn.ai.agent.ScriptMasterAgent;
import com.wn.config.AgentFactory;
import com.wn.dto.ScriptGenRequest;
import com.wn.entity.AiScriptTask;
import com.wn.mapper.AiScriptTaskMapper;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 剧本生成服务
 *
 * 【AgentScope知识点26：异步任务处理】
 * 剧本生成可能需要几分钟，不能让前端一直等着。
 * 所以用RabbitMQ做异步：
 * 1. 前端提交请求 → 创建任务记录 → 发消息到MQ
 * 2. 消费者接收消息 → 调用Agent生成 → 更新任务状态
 * 3. 前端轮询或者WebSocket通知结果
 *
 * AgentScope本身是同步的（call()方法返回Mono），
 * 但我们可以把它放到异步任务里执行。
 */
@Slf4j
@Service
public class ScriptGenService {

    private final AgentFactory agentFactory;
    private final AiScriptTaskMapper taskMapper;

    public ScriptGenService(AgentFactory agentFactory, AiScriptTaskMapper taskMapper) {
        this.agentFactory = agentFactory;
        this.taskMapper = taskMapper;
    }

    public Long submitTask(ScriptGenRequest request, Long userId) {
        AiScriptTask task = new AiScriptTask();
        task.setUserId(userId);
        task.setScriptTheme(request.getTheme());
        task.setScriptType(request.getType());
        task.setPlayerCount(request.getPlayerCount());
        task.setDifficulty(request.getDifficulty());
        task.setBackgroundDesc(request.getBackgroundDesc());
        task.setTaskStatus(0);
        task.setProgress(0);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        taskMapper.insert(task);
        return task.getTaskId();
    }

    public AiScriptTask getTaskStatus(Long taskId) {
        return taskMapper.selectById(taskId);
    }

    public Mono<String> generateScript(String theme, String type, int playerCount, int difficulty, String background) {
        String userInput = String.format("主题：%s，类型：%s，人数：%d，难度：%d，背景：%s",
                theme, type, playerCount, difficulty, background);

        PlotDesignAgent agent = agentFactory.createPlotDesignAgent();
        ContentBlock block = TextBlock.builder().text(userInput).build();
        Msg inputMsg = Msg.builder().content(block).build();

        return agent.call(inputMsg)
                .map(result -> {
                    List<ContentBlock> blocks = result.getContent();
                    return blocks.stream()
                            .filter(b -> b instanceof TextBlock)
                            .map(b -> ((TextBlock) b).getText())
                            .collect(Collectors.joining("\n"));
                });
    }

    public Mono<String> generateRoles(String plotOutline) {
        RoleDesignAgent agent = agentFactory.createRoleDesignAgent();
        ContentBlock block = TextBlock.builder().text(plotOutline).build();
        Msg inputMsg = Msg.builder().content(block).build();

        return agent.call(inputMsg)
                .map(result -> {
                    List<ContentBlock> blocks = result.getContent();
                    return blocks.stream()
                            .filter(b -> b instanceof TextBlock)
                            .map(b -> ((TextBlock) b).getText())
                            .collect(Collectors.joining("\n"));
                });
    }
}
