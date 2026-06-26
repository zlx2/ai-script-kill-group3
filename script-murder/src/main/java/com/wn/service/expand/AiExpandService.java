package com.wn.service.expand;

import com.wn.entity.expand.ExpandVO;
import reactor.core.publisher.Flux;

public interface AiExpandService {
    Flux<ExpandVO> expand(String text);
}
