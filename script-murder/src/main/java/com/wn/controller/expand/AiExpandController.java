package com.wn.controller.expand;

import com.wn.service.expand.AiExpandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI扩写控制器，用于对接前端调用AI扩写接口
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiExpandController {
    private final AiExpandService aiExpandService;
    /**
     * 调用AI扩写接口
     */
    @RequestMapping(value = "/expand", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> expand(@RequestParam("text") String text) {
        return aiExpandService.expand(text);
    }

}
