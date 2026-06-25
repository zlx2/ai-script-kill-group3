package com.wn.mapper.dm;

import com.wn.entity.dm.ScriptReviewPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @Author: 杜江
 * @Description: dm剧本审核实体
 * @DateTime: 2026/6/25 11:32
 * @Component:
 **/
@Repository
public interface DmScriptReviewMapper extends JpaRepository<ScriptReviewPO, Long> {
    Optional<ScriptReviewPO> findByScriptId(Long scriptId);
}
