/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/22 15:03
 * @Component:
 **/
package com.wn.mapper.script;

import com.wn.entity.script.ScriptPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ScriptMapper extends JpaRepository<ScriptPO, Long> , JpaSpecificationExecutor<ScriptPO> {

}
