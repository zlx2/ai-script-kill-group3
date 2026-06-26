/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 16:51
 * @Component:
 **/
package com.wn.mapper.script;

import com.wn.entity.script.questions.GameQuestionPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameQuestionRepository extends JpaRepository<GameQuestionPO, Long> {
    // 根据剧本+角色ID查询题目
    List<GameQuestionPO> findByScriptIdAndRoleIdOrderBySortNumAsc(Long scriptId, Long roleId);
    // 根据剧本查询所有题目
    List<GameQuestionPO> findByScriptId(Long scriptId);
}
