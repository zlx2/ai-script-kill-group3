package com.wn.service.expand;

import reactor.core.publisher.Flux;

public interface AiExpandService {
    Flux<String> expand(String text);
}
