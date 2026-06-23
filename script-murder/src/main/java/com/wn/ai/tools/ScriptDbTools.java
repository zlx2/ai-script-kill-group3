package com.wn.ai.tools;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 17:44
 * @Component:
 **/

import com.wn.entity.ScriptInfo;
import com.wn.service.ScriptInfoService;
import io.agentscope.core.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 剧本数据库查询工具
 * 供Agent调用，查询已有的剧本作为参考
 *
 * 【AgentScope知识点24：@Tool注解 - 定义工具】
 * 用@Tool注解标记一个方法，AgentScope会自动：
 * 1. 解析方法签名
 * 2. 生成JSON Schema描述
 * 3. 把工具注册到Agent上
 * 4. 大模型可以自动识别并调用这个工具
 *
 * 就像你给工具贴个标签，写清楚"这是干什么用的，怎么用"，
 * Agent一看就知道什么时候该用这个工具。
 *
 * 注意：工具方法的参数和返回值要尽量简单，
 * 大模型才能正确理解和调用。
 */
@Component
public class ScriptDbTools {

    private final ScriptInfoService scriptInfoService;

    public ScriptDbTools(ScriptInfoService scriptInfoService) {
        this.scriptInfoService = scriptInfoService;
    }

    /**
     * 查询参考剧本
     * @param scriptType 剧本类型
     * @param limit 返回数量
     * @return 参考剧本列表
     *
     * 【AgentScope知识点25：工具调用流程】
     * Agent调用工具的完整流程：
     * 1. Agent（大模型）决定要调用工具
     * 2. 大模型输出工具名和参数（JSON格式）
     * 3. AgentScope解析工具调用请求
     * 4. 反射调用对应的Java方法
     * 5. 把方法返回值包装成Observation
     * 6. 把Observation加回上下文，继续推理
     *
     * 整个过程是自动的，你只需要写工具方法就行。
     */
@Tool(name = "queryReferenceScripts", description = "根据类型查询参考剧本，用于生成新剧本时参考")
    public String queryReferenceScripts(String scriptType, int limit) {
        List<ScriptInfo> scripts = scriptInfoService.lambdaQuery()
                .eq(ScriptInfo::getScriptType, scriptType)
                .eq(ScriptInfo::getStatus, 1) // 已上架
                .last("LIMIT " + limit)
                .list();

        if (scripts.isEmpty()) {
            return "暂无参考剧本";
        }

        return scripts.stream()
                .map(s -> String.format("- 《%s》：%s", s.getScriptName(), s.getDescription()))
                .collect(Collectors.joining("\n"));
    }
}
