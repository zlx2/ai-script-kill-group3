package com.wn.mapper.script;

import com.wn.entity.script.AiScriptTaskPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @Author: 杜江
 * @Description:剧本任务Mapper接口
 * @DateTime: 2026/6/24 10:45
 * @Component:
 **/
@Repository
public interface AiScriptTaskMapper extends JpaRepository<AiScriptTaskPO, Long> {
}
