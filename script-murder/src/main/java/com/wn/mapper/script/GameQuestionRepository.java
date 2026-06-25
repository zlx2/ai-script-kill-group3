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
    // 根据角色查询题目，按序号升序
    List<GameQuestionPO> findByRoleTypeOrderBySortNumAsc(String roleType);
}
