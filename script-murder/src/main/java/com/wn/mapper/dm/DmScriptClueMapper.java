package com.wn.mapper.dm;

import com.wn.entity.clue.ScriptCluePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: 杜江
 * @Description: dm剧本线索实体
 * @DateTime: 2026/6/25 11:31
 * @Component:
 **/
@Repository
public interface DmScriptClueMapper extends JpaRepository<ScriptCluePO, Long> {
    List<ScriptCluePO> findByScriptId(Long scriptId);
    List<ScriptCluePO> findByScriptIdAndActOrder(Long scriptId, Integer actOrder);
}
