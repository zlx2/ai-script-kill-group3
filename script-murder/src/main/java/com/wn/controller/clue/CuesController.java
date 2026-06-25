package com.wn.controller.clue;

import com.wn.entity.R;
import com.wn.entity.clue.ScriptCluePO;
import com.wn.service.clue.ClueService;
import com.wn.utils.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 线索的控制器
 */
@RestController
@RequestMapping("/cues")
@RequiredArgsConstructor
public class CuesController {
    private final ClueService clueService;

    /**
     * 查询该角色的所有线索（公开，私有都查，以字段形式分辨返回）
     */
    @RequestMapping("/all")
    public List<ScriptCluePO> getAllClues(Long roleId, int scene) {
        return clueService.getAllClues(roleId, scene);
    }

    /**
     * 公开所选线索
     */
    @RequestMapping("/open")
    public R openClue(Long clueId) {
        clueService.openClue(clueId);
        return R.SUCCESS;
    }
}
