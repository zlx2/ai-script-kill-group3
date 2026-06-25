package com.wn.service.impl.clue;

import com.wn.entity.clue.ScriptCluePO;
import com.wn.mapper.clue.ClueMapper;
import com.wn.service.clue.ClueService;
import com.wn.utils.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 线索服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClueServiceImpl implements ClueService {

    private final ClueMapper clueMapper;
    /**
     * 获取所有线索，包括公开和未公开的线索
     *  获取的条件：
     *      1.同一个剧本下的当前角色拥有的线索
     *      2.当前场景下的线索，包括已经经历过的场景和已知的线索
     */
    @Override
    public List<ScriptCluePO> getAllClues(Long roleId, int scene) {
        if (roleId == null || scene == 0 || scene > 10) {
            throw new GlobalException(400,"角色ID或场景不合规");
        }


        return clueMapper.findByRoleIdAndScene(roleId, scene);
    }

    /**
     * 公开所选线索
     * @param clueId
     */
    @Override
    public void openClue(Long clueId) {
        if (clueId == null) {
            throw new GlobalException(400,"线索ID不能为空");
        }
        ScriptCluePO clue = clueMapper.findById(clueId).orElse(null);
        if (clue == null) {
            throw new GlobalException(404,"线索不存在");
        }
        clue.setIsHidden(1);//0隐藏 1公开
        clueMapper.save(clue);
    }
}
