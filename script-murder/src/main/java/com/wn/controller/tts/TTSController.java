package com.wn.controller.tts;

import com.wn.entity.R;
import com.wn.service.tts.TtsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 文本转语音控制器
 */
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TTSController {
    private final TtsService ttsService;

    @PostMapping("/speak")
    public R speak(@RequestBody Map<String, Object> body) {
        return ttsService.synthesize(body);
    }

}
