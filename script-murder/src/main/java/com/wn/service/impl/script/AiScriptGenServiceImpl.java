package com.wn.service.impl.script;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wn.entity.script.*;
import com.wn.mapper.script.AiScriptOutlineMapper;
import com.wn.mapper.script.AiScriptTaskMapper;
import com.wn.mapper.script.ScriptMapper;
import com.wn.mapper.script.ScriptRoleMapper;
import com.wn.service.script.AiScriptGenService;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;




import java.util.concurrent.CompletableFuture;

/**
 * @Author: 杜江
 * @Description:AI剧本杀服务类实现类
 * @DateTime: 2026/6/24 11:03
 * @Component:
 **/
@Slf4j
@Service
public class AiScriptGenServiceImpl implements AiScriptGenService {
    @Resource
    private AiScriptOutlineMapper aiScriptOutlineMapper;

    @Resource
    private HarnessAgent plotDesignAgent;

    @Resource
    private HarnessAgent roleDesignAgent;

    @Resource
    private ScriptMapper scriptMapper;

    @Resource
    private ScriptRoleMapper scriptRoleMapper;

    @Resource
    private AiScriptTaskMapper taskMapper;

    // 提交剧本生成任务
    @Override
    public Long submitTask(ScriptGenRequest request, Long userId) {
        AiScriptTaskPO task = AiScriptTaskPO.builder()
                .userId(userId)
                .taskStatus(0)
                .progress(0)
                .scriptTheme(request.getTheme())
                .scriptType(request.getScriptType())
                .playerCount(String.valueOf(request.getPlayerCount()))
                .difficulty("2")  // 默认难度：中等
                .build();
        taskMapper.save(task);

        CompletableFuture.runAsync(() -> {
            try {
                updateProgress(task.getTaskId(), 1, 25);

                String outline = generateScript(request).block();
                log.info("AI返回的剧本大纲原始内容: {}", outline);
                String cleanOutline = extractPureJson(outline);
                log.info("提取后的JSON: {}", cleanOutline);

                JSONObject outlineJson;
                if (cleanOutline.trim().startsWith("[")) {
                    // 如果是数组，取第一个元素
                    JSONArray array = JSON.parseArray(cleanOutline);
                    if (array.size() > 0) {
                        outlineJson = array.getJSONObject(0);
                    } else {
                        log.error("JSON数组为空");
                        throw new RuntimeException("剧本大纲生成失败：返回空数组");
                    }
                } else {
                    outlineJson = JSON.parseObject(cleanOutline);
                }
                log.info("解析后的theme: {}", outlineJson.getString("theme"));
                log.info("解析后的outline: {}", outlineJson.getString("outline"));
                log.info("解析后的scriptName: {}", outlineJson.getString("scriptName"));

                ScriptPO scriptPO = new ScriptPO();
                scriptPO.setScriptName(outlineJson.getString("scriptName"));
                scriptPO.setScriptType(outlineJson.getString("scriptType"));
                scriptPO.setDescription(outlineJson.getString("description"));
                scriptPO.setBackgroundStory(outlineJson.getString("backgroundStory"));
                scriptPO.setPlayerCount(outlineJson.getIntValue("playerCount"));
                scriptPO.setIsAiGenerated((byte) 1);
                scriptPO.setStatus((byte) 0);

                ScriptPO savedScript = scriptMapper.save(scriptPO);
                task.setScriptId(savedScript.getScriptId());
                taskMapper.save(task);

                // 保存剧本大纲
                AiScriptOutlinePO outlinePO = AiScriptOutlinePO.builder()
                        .scriptId(savedScript.getScriptId())
                        .theme(outlineJson.getString("theme") != null ? outlineJson.getString("theme") : request.getTheme())
                        .outline(outlineJson.getString("outline") != null ? outlineJson.getString("outline") : outlineJson.getString("description"))
                        .backgroundStory(outlineJson.getString("backgroundStory"))
                        .coreTrick(outlineJson.getString("coreTrick"))
                        .build();
                aiScriptOutlineMapper.save(outlinePO);

                updateProgress(task.getTaskId(), 1, 75);

                String rolesJson = generateRoles(outline).block();
                saveRoles(savedScript.getScriptId(), rolesJson);

                updateProgress(task.getTaskId(), 2, 100);

                log.info("剧本生成完成，taskId={}, scriptId={}", task.getTaskId(), savedScript.getScriptId());
            } catch (Exception e) {
                AiScriptTaskPO failedTask = taskMapper.findById(task.getTaskId()).orElse(task);
                failedTask.setTaskStatus(-1);
                failedTask.setErrorMsg(e.getMessage());
                taskMapper.save(failedTask);
                log.error("剧本生成失败", e);
            }
        });

        return task.getTaskId();
    }

    @Override
    public AiScriptTaskPO getTaskStatus(Long taskId) {
        return taskMapper.findById(taskId).orElse(null);
    }

    private void updateProgress(Long taskId, int status, int progress) {
        AiScriptTaskPO task = taskMapper.findById(taskId).orElse(null);
        if (task != null) {
            task.setTaskStatus(status);
            task.setProgress(progress);
            taskMapper.save(task);
        }
    }

    private Mono<String> generateScript(ScriptGenRequest request) {
        String skillContent = readSkillFile("skill/plot_design_skill.md");
        String difficulty = request.getDifficulty() != null ? request.getDifficulty() : "中等";

        String prompt = String.format("""
                %s

                用户需求：
                主题：%s
                类型：%s
                人数：%d人
                难度：%s
                背景描述：%s
                """, skillContent, request.getTheme(), request.getScriptType(),
                request.getPlayerCount(), difficulty, request.getDescription());

        Msg msg = Msg.builder()
                .content(TextBlock.builder().text(prompt).build())
                .build();

        return plotDesignAgent.call(msg)
                .map(result -> result.getContent().stream()
                        .filter(b -> b instanceof TextBlock)
                        .map(b -> ((TextBlock) b).getText())
                        .collect(java.util.stream.Collectors.joining("\n")));
    }

    private Mono<String> generateRoles(String outline) {
        String skillContent = readSkillFile("skill/role_design_skill.md");

        String prompt = String.format("""
                %s

                请根据以下剧本大纲设计角色信息：
                %s
                """, skillContent, outline);

        Msg msg = Msg.builder()
                .content(TextBlock.builder().text(prompt).build())
                .build();

        return roleDesignAgent.call(msg)
                .map(result -> result.getContent().stream()
                        .filter(b -> b instanceof TextBlock)
                        .map(b -> ((TextBlock) b).getText())
                        .collect(java.util.stream.Collectors.joining("\n")));
    }

    private String readSkillFile(String filePath) {
        try {
            org.springframework.core.io.ClassPathResource resource =
                    new org.springframework.core.io.ClassPathResource(filePath);
            if (!resource.exists()) {
                log.warn("Skill 文件不存在: {}", filePath);
                return "";
            }
            try (java.io.InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (java.io.IOException e) {
            log.error("读取 skill 文件失败: {}", filePath, e);
            return "";
        }
    }

    private void saveRoles(Long scriptId, String rolesJson) {
        log.info("AI返回的角色内容: {}", rolesJson);

        String cleanJson = extractPureJson(rolesJson);
        log.info("提取后的JSON: {}", cleanJson);  // ← 打印提取后的内容

        if (cleanJson == null || cleanJson.isEmpty()) {
            log.error("提取的JSON为空");
            return;
        }

        try {
            // 尝试解析为对象
            if (cleanJson.trim().startsWith("{")) {
                JSONObject root = JSON.parseObject(cleanJson);

                if (root.containsKey("roles")) {
                    JSONArray rolesArray = root.getJSONArray("roles");
                    for (int i = 0; i < rolesArray.size(); i++) {
                        saveSingleRole(scriptId, rolesArray.getJSONObject(i));
                    }
                } else {
                    log.warn("JSON对象中没有roles字段");
                }
            }
            // 尝试解析为数组
            else if (cleanJson.trim().startsWith("[")) {
                JSONArray rolesArray = JSON.parseArray(cleanJson);
                for (int i = 0; i < rolesArray.size(); i++) {
                    saveSingleRole(scriptId, rolesArray.getJSONObject(i));
                }
            }
            // 都不是
            else {
                log.error("无法识别的JSON格式: {}", cleanJson);
            }
        } catch (Exception e) {
            log.error("保存角色失败", e);
        }
    }

    private void saveSingleRole(Long scriptId, JSONObject roleNode) {
        ScriptRolePO role = new ScriptRolePO();
        role.setScriptId(scriptId);
        role.setRoleName(roleNode.getString("roleName"));
        role.setGender(roleNode.getString("gender"));

        // 处理 age 字段（可能是字符串或数字）
        try {
            role.setAge(roleNode.getIntValue("age"));
        } catch (Exception e) {
            role.setAge(0);
        }

        role.setCharacterStory(roleNode.getString("characterStory"));
        role.setSecretInfo(roleNode.getString("secretInfo"));

        scriptRoleMapper.save(role);
        log.info("保存角色成功: {}", role.getRoleName());
    }

    private String extractPureJson(String content) {
        if (content == null || content.isEmpty()) return "";

        content = content.replaceAll("```json\\s*", "");
        content = content.replaceAll("```\\s*", "");
        content = content.replaceAll("(?m)^#.*$", "");
        content = content.trim();

        int startIndex = content.indexOf('[');
        if (startIndex >= 0) {
            int bracketCount = 0;
            for (int i = startIndex; i < content.length(); i++) {
                if (content.charAt(i) == '[') bracketCount++;
                if (content.charAt(i) == ']') bracketCount--;
                if (bracketCount == 0) {
                    return content.substring(startIndex, i + 1);
                }
            }
        }

        startIndex = content.indexOf('{');
        if (startIndex >= 0) {
            int braceCount = 0;
            for (int i = startIndex; i < content.length(); i++) {
                if (content.charAt(i) == '{') braceCount++;
                if (content.charAt(i) == '}') braceCount--;
                if (braceCount == 0) {
                    return content.substring(startIndex, i + 1);
                }
            }
        }

        // 如果找不到完整的JSON，尝试提取第一个对象
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return content.substring(firstBrace, lastBrace + 1);
        }

        return content;
    }
}
