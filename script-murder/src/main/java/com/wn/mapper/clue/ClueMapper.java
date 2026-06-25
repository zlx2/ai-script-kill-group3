package com.wn.mapper.clue;

import com.wn.entity.clue.ScriptCluePO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 线索映射器
 */
public interface ClueMapper extends JpaRepository<ScriptCluePO, Long> {

    List<ScriptCluePO> findByRoleIdAndScene(Long roleId, String scene);
}
