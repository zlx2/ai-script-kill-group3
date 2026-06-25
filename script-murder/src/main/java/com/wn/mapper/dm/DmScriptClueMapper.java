/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/25 14:23
 * @Component:
 **/
package com.wn.mapper.dm;

import com.wn.entity.clue.ScriptCluePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DmScriptClueMapper extends JpaRepository<ScriptCluePO, Long> {

    // 查询剧本所有线索
    List<ScriptCluePO> findByScriptId(Long scriptId);

    // 查询某一幕的线索
    List<ScriptCluePO> findByScriptIdAndActOrder(Long scriptId, Integer actOrder);
}
