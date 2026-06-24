package com.wn.mapper.script;

import com.wn.entity.script.ScriptRolePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: 杜江
 * @Description:剧本角色Mapper接口
 * @DateTime: 2026/6/24 10:42
 * @Component:
 **/
@Repository
public interface ScriptRoleMapper extends JpaRepository<ScriptRolePO, Long> {
    List<ScriptRolePO> findByScriptId(Long scriptId);
}
