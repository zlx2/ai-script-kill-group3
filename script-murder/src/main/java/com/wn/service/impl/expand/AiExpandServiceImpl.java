package com.wn.service.impl.expand;

import com.wn.service.expand.AiExpandService;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class AiExpandServiceImpl implements AiExpandService {
    private final HarnessAgent agent;
    public AiExpandServiceImpl(@Qualifier("expandAgent") HarnessAgent agent) {
        this.agent = agent;
    }

    /**
     * 调用AI扩写接口,流式回复
     * @param originalText 原始文本
     * @return 扩写后的文本流
     */
    @Override
    public Flux<String> expand(String originalText) {
        // 1. 构造用户消息
        String prompt = "请扩写以下内容，要求逻辑明了,语句通顺，表达清晰：\n" + originalText;
        Msg userMsg = Msg.builder()
                .content(TextBlock.builder().text(prompt).build())
                .build();
        // 2. 调用 AgentScope 的流式 API
        //    stream() 返回 Flux<AgentEvent>
        return agent.streamEvents(userMsg, RuntimeContext.empty())
                // 3. 过滤出文本增量事件（每个 Token）
                .filter(event -> event instanceof TextBlockDeltaEvent)
                // 4. 提取增量文本
                .map(event -> ((TextBlockDeltaEvent) event).getDelta())
                // 5. 异常处理：出错时返回错误信息
                .onErrorResume(e -> {
                    log.error("AI 扩写流式处理出错", e);
                    return Flux.just(" [服务出错，请稍后重试] ");
                })
                .doOnComplete(() -> log.info("AI 扩写完成"))
                .doOnCancel(() -> log.info("AI 扩写被取消"));
    }
}

