package com.wn.service;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:30
 * @Component:
 **/

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wn.ai.agent.PlotDesignAgent;
import com.wn.ai.agent.RoleDesignAgent;
import com.wn.ai.agent.ScriptMasterAgent;
import com.wn.config.AgentFactory;
import com.wn.dto.ScriptGenRequest;
import com.wn.entity.AiScriptTask;
import com.wn.entity.ScriptInfo;
import com.wn.entity.ScriptRole;
import com.wn.mapper.AiScriptTaskMapper;
import com.wn.mapper.ScriptInfoMapper;
import com.wn.mapper.ScriptRoleMapper;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    private final ScriptInfoMapper scriptInfoMapper;
    private final ScriptRoleMapper scriptRoleMapper;
    private final ObjectMapper objectMapper;

    public ScriptGenService(AgentFactory agentFactory,
                            AiScriptTaskMapper taskMapper,
                            ScriptInfoMapper scriptInfoMapper,
                            ScriptRoleMapper scriptRoleMapper,
                            ObjectMapper objectMapper) {
        this.agentFactory = agentFactory;
        this.taskMapper = taskMapper;
        this.scriptInfoMapper = scriptInfoMapper;
        this.scriptRoleMapper = scriptRoleMapper;
        this.objectMapper = objectMapper;
    }

    public Long submitTask(ScriptGenRequest request, Long userId) {
        AiScriptTask task = AiScriptTask.builder()
                .userId(userId)
                .scriptTheme(request.getTheme())
                .scriptType(request.getType())
                .playerCount(request.getPlayerCount())
                .difficulty(request.getDifficulty())
                .backgroundDesc(request.getBackgroundDesc())
                .taskStatus(0)
                .progress(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        taskMapper.insert(task);

        CompletableFuture.runAsync(() -> {
            try {
                // 阶段1：生成剧本大纲 (25%)
                task.setTaskStatus(1);
                task.setProgress(25);
                task.setUpdateTime(LocalDateTime.now());
                taskMapper.updateById(task);

                String outline = generateScript(
                        request.getTheme(),
                        request.getType(),
                        request.getPlayerCount(),
                        request.getDifficulty(),
                        request.getBackgroundDesc()
                ).block();

                // 阶段2：保存剧本 (50%)
                ScriptInfo scriptInfo = ScriptInfo.builder()
                        .scriptName(request.getTheme())
                        .scriptType(request.getType())
                        .theme(request.getTheme())
                        .playerCount(request.getPlayerCount())
                        .outline(outline)
                        .description(request.getBackgroundDesc())
                        .status(1)
                        .build();
                scriptInfoMapper.insert(scriptInfo);

                task.setScriptId(scriptInfo.getScriptId());
                task.setProgress(50);
                task.setUpdateTime(LocalDateTime.now());
                taskMapper.updateById(task);

                // 阶段3：生成角色 (75%)
                String roles = generateRoles(outline).block();

                // 解析角色JSON并保存
                saveRoles(scriptInfo.getScriptId(), roles);

                task.setProgress(75);
                task.setUpdateTime(LocalDateTime.now());
                taskMapper.updateById(task);

                // 阶段4：完成 (100%)
                task.setProgress(100);
                task.setTaskStatus(2);
                task.setUpdateTime(LocalDateTime.now());
                taskMapper.updateById(task);

                log.info("剧本生成完成，taskId={}, scriptId={}", task.getTaskId(), scriptInfo.getScriptId());

            } catch (Exception e) {
                log.error("剧本生成失败", e);
                task.setTaskStatus(-1);
                task.setErrorMsg(e.getMessage());
                task.setUpdateTime(LocalDateTime.now());
                taskMapper.updateById(task);
            }
        });

        return task.getTaskId();
    }

    private void saveRoles(Long scriptId, String rolesJson) {
        try {
            // 清洗 JSON，去除 Markdown 冗余内容
            String cleanJson = extractPureJson(rolesJson);

            if (cleanJson.isEmpty()) {
                log.warn("清洗后JSON为空，保存原始文本");
                ScriptRole role = ScriptRole.builder()
                        .scriptId(scriptId)
                        .roleName("角色列表")
                        .characterStory(rolesJson)
                        .build();
                scriptRoleMapper.insert(role);
                return;
            }

            JsonNode root = objectMapper.readTree(cleanJson);
            if (root.isArray()) {
                for (JsonNode roleNode : root) {
                    ScriptRole role = ScriptRole.builder()
                            .scriptId(scriptId)
                            .roleName(roleNode.hasNonNull("roleName") ? roleNode.get("roleName").asText("未知角色") : "未知角色")
                            .gender(roleNode.hasNonNull("gender") ? roleNode.get("gender").asText("未知") : "未知")
                            .age(roleNode.hasNonNull("age") ? roleNode.get("age").asInt(0) : 0)
                            .characterStory(roleNode.hasNonNull("characterStory") ? roleNode.get("characterStory").asText("") : "")
                            .secretInfo(roleNode.hasNonNull("secretInfo") ? roleNode.get("secretInfo").asText("") : "")
                            .build();
                    scriptRoleMapper.insert(role);
                }
            } else {
                log.warn("JSON不是数组格式，保存原始文本");
                ScriptRole role = ScriptRole.builder()
                        .scriptId(scriptId)
                        .roleName("角色列表")
                        .characterStory(rolesJson)
                        .build();
                scriptRoleMapper.insert(role);
            }
        } catch (Exception e) {
            log.warn("解析角色JSON失败，保存原始文本", e);
            ScriptRole role = ScriptRole.builder()
                    .scriptId(scriptId)
                    .roleName("角色列表")
                    .characterStory(rolesJson)
                    .build();
            scriptRoleMapper.insert(role);
        }
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

    /**
     * 添加JSON清洗方法，用于提取纯JSON字符串
     * @param content
     * @return
     */

    private String extractPureJson(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 去除 Markdown 标题 (#, ##, ### 等)
        content = content.replaceAll("^#{1,6}\\s+", "");

        // 去除 markdown 代码块标记（```json 或 ```）
        content = content.replaceAll("```json\\s*", "");
        content = content.replaceAll("```\\s*", "");

        // 去除首尾空格和换行
        content = content.trim();

        // 查找 JSON 数组的起始和结束位置
        int startIndex = content.indexOf('[');
        int endIndex = content.lastIndexOf(']');

        if (startIndex >= 0 && endIndex >= startIndex) {
            return content.substring(startIndex, endIndex + 1);
        }

        // 如果找不到数组，尝试找对象
        startIndex = content.indexOf('{');
        endIndex = content.lastIndexOf('}');

        if (startIndex >= 0 && endIndex >= startIndex) {
            return content.substring(startIndex, endIndex + 1);
        }

        return content;
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
