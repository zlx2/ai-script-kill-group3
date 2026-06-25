package com.wn.mapper.dm;

import com.wn.entity.dm.ScriptHintPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: 杜江
 * @Description: dm扶车提示实体
 * @DateTime: 2026/6/25 11:31
 * @Component:
 **/
@Repository
public interface DmScriptHintMapper extends JpaRepository<ScriptHintPO, Long> {
    List<ScriptHintPO> findByScriptId(Long scriptId);
    List<ScriptHintPO> findByScriptIdAndActOrder(Long scriptId, Integer actOrder);
    List<ScriptHintPO> findByScriptIdAndHintLevel(Long scriptId, Byte hintLevel);
}
