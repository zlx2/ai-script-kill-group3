package com.wn.controller.clue;

import com.wn.service.clue.ClueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 线索的控制器
 *  1.查询所有公开线索
 *  2.查询私有未公开线索
 *  3.私有线索公开化
 */
@RestController
@RequestMapping("/cues")
@RequiredArgsConstructor
public class CuesController {
    private final ClueService clueService;


}
