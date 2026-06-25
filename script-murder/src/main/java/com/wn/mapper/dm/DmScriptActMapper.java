package com.wn.mapper.dm;


import com.wn.entity.dm.ScriptActPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: 杜江
 * @Description: dm剧本事件实体
 * @DateTime: 2026/6/25 11:30
 * @Component:
 **/
@Repository
public interface DmScriptActMapper extends JpaRepository<ScriptActPO, Long> {
    List<ScriptActPO> findByScriptIdOrderByActOrderAsc(Long scriptId);
}
