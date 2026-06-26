/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 14:43
 * @Component:
 **/
package com.wn.mapper.script;

import com.wn.entity.script.stage.ScriptStagePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 剧本分幕阶段Mapper
 */
@Repository
public interface ScriptStageMapper extends JpaRepository<ScriptStagePO, Long> {
    List<ScriptStagePO> findByScriptIdOrderByStageNoAsc(Long scriptId);
    // 基础CRUD由JpaRepository自带：save、findById、findAll、deleteById等
    // 后续需要按scriptId查询分幕再补充自定义方法
}
