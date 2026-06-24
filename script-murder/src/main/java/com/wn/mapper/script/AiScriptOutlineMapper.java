package com.wn.mapper.script;

import com.wn.entity.script.AiScriptOutlinePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/24 12:12
 * @Component:
 **/
@Repository
public interface AiScriptOutlineMapper extends JpaRepository<AiScriptOutlinePO, Long> {
    Optional<AiScriptOutlinePO> findByScriptId(Long scriptId);
}
