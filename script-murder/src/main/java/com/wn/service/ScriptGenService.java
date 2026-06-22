package com.wn.service;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:30
 * @Component:
 **/

import com.wn.ai.agent.ScriptMasterAgent;
import com.wn.config.AgentFactory;
import com.wn.dto.ScriptGenRequest;
import com.wn.entity.AiScriptTask;
import com.wn.mapper.AiScriptTaskMapper;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

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
@Service
public class ScriptGenService {

    private final AgentFactory agentFactory;
    private final AiScriptTaskMapper aiScriptTaskMapper;
    private final ScriptInfoService scriptInfoService;

    public ScriptGenService(AgentFactory agentFactory,
                            AiScriptTaskMapper aiScriptTaskMapper,
                            ScriptInfoService scriptInfoService) {
        this.agentFactory = agentFactory;
        this.aiScriptTaskMapper = aiScriptTaskMapper;
        this.scriptInfoService = scriptInfoService;
    }

    /**
     * 提交剧本生成任务
     */
    public Long submitTask(ScriptGenRequest request, Long userId) {
        // 1. 创建任务记录
        AiScriptTask task = new AiScriptTask();
        task.setUserId(userId);
        task.setScriptTheme(request.getTheme());
        task.setScriptType(request.getType());
        task.setPlayerCount(request.getPlayerCount());
        task.setDifficulty(request.getDifficulty());
        task.setBackgroundDesc(request.getBackgroundDesc());
        task.setTaskStatus(0); // 排队中
        task.setProgress(0);
        aiScriptTaskMapper.insert(task);

        // 2. 发送到RabbitMQ（这里简化，直接调用）
        // 实际项目中用rabbitTemplate.convertAndSend()
        // generateScriptAsync(task.getTaskId());

        return task.getTaskId();
    }

    /**
     * 异步生成剧本（RabbitMQ消费者）
     *
     * 【AgentScope知识点27：Agent的调用方式】
     * 调用Agent的核心就是call()方法：
     * - 输入：Msg对象（包含用户输入的内容）
     * - 输出：Mono<Msg>（响应式，包含Agent的回复）
     *
     * Msg是AgentScope的消息对象，不只有文本，
     * 还可以携带图片、音频、结构化数据等。
     */
    @RabbitListener(queues = "script-gen-queue")
    public void generateScriptAsync(Long taskId) {
        // 1. 查询任务
        AiScriptTask task = aiScriptTaskMapper.selectById(taskId);
        if (task == null) return;

        // 2. 更新状态为生成中
        task.setTaskStatus(1);
        task.setProgress(5);
        aiScriptTaskMapper.updateById(task);

        try {
            // 3. 创建总控Agent
            ScriptMasterAgent masterAgent = agentFactory.createScriptMasterAgent();

            // 4. 构建输入消息
            String userInput = String.format(
                    "请生成一个剧本杀剧本：\n" +
                            "主题：%s\n" +
                            "类型：%s\n" +
                            "玩家人数：%d人\n" +
                            "难度：%s\n" +
                            "额外要求：%s",
                    task.getScriptTheme(),
                    task.getScriptType(),
                    task.getPlayerCount(),
                    task.getDifficulty() == 1 ? "简单" : (task.getDifficulty() == 2 ? "中等" : "困难"),
                    task.getBackgroundDesc()
            );

            ContentBlock block = TextBlock.builder().text(userInput).build();
            Msg inputMsg = Msg.builder().content(block).build();

            // 5. 调用Agent生成剧本（阻塞等待结果）
            // 【AgentScope知识点28：Mono响应式】
            // AgentScope用Project Reactor的Mono/Flux做响应式编程，
            // .block()就是阻塞等待结果。
            // 也可以用.subscribe()异步处理，不阻塞。
            Msg resultMsg = masterAgent.call(inputMsg).block();

            // 6. 解析结果，写入数据库
            List<ContentBlock> blocks = resultMsg.getContent();
            String scriptContent = blocks.stream()
                    .filter(b -> b instanceof TextBlock)
                    .map(b -> ((TextBlock) b).getText())
                    .collect(Collectors.joining("\n"));

            // （这里省略解析JSON的逻辑，实际项目中用ObjectMapper解析）
            // 解析后写入script_info、script_role、script_clue等表

            // 7. 更新任务状态为完成
            task.setTaskStatus(2);
            task.setProgress(100);
            // task.setScriptId(generatedScriptId);
            aiScriptTaskMapper.updateById(task);

            System.out.println("剧本生成完成，任务ID：" + taskId);

        } catch (Exception e) {
            // 8. 失败处理
            task.setTaskStatus(3);
            task.setErrorMsg(e.getMessage());
            aiScriptTaskMapper.updateById(task);
            e.printStackTrace();
        }
    }

    /**
     * 查询任务状态
     */
    public AiScriptTask getTaskStatus(Long taskId) {
        return aiScriptTaskMapper.selectById(taskId);
    }
}
