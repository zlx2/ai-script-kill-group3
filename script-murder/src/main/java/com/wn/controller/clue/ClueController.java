package com.wn.controller.clue;

import com.wn.entity.R;
import com.wn.entity.clue.ScriptCluePO;
import com.wn.entity.dm.RoomCluePO;
import com.wn.service.clue.ClueService;
import com.wn.utils.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 线索的控制器
 */
@RestController
@RequestMapping("/clue")
@RequiredArgsConstructor
public class ClueController {
    private final ClueService clueService;

    /**
     * 查询该角色的所有线索（公开，私有都查，以字段形式分辨返回）
     * @param roleId        角色id
     * @param scene         场景
     * @param roomId        房间号
     * @return 线索列表
     */
    @GetMapping("/all")
    public List<ScriptCluePO> getAllClues(@RequestParam Long roleId, @RequestParam int scene, @RequestParam String roomId) {
        return clueService.getAllClues(roleId, scene, roomId);
    }

    /**
     * 公开线索，成功后返回新的线索列表
     * @param roomId    房间号
     * @param clueId    线索id
     * @param userId    用户id
     * @param roleId    角色id
     * @param scene     场景
     * @return
     */
    @PostMapping("/open")
    public List<ScriptCluePO> openClue(@RequestParam String roomId,
                                       @RequestParam Long clueId,
                                       @RequestParam Long userId,
                                       @RequestParam Long roleId,
                                       @RequestParam int scene) {
        return clueService.openClue(roomId, clueId, userId, roleId, scene);
    }


    @PostMapping("/add")
    public R addClue(@RequestBody ScriptCluePO scriptCluePO) {
        clueService.addClue(scriptCluePO);
        return R.SUCCESS;
    }
}
