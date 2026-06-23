/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/23 11:05
 * @Component:
 **/
package com.wn.mapper.script;

import com.wn.entity.script.ScriptPOBackups;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScriptBackupsMapper extends JpaRepository<ScriptPOBackups, Long> {
}
