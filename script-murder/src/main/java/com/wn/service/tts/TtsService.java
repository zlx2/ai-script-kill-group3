package com.wn.service.tts;

import com.wn.entity.R;

import java.util.Map;

public interface TtsService {
    R synthesize(Map<String, Object> body);
}
