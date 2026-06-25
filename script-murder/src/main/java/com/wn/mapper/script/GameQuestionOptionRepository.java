/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 16:54
 * @Component:
 **/
package com.wn.mapper.script;

import com.wn.entity.script.questions.GameQuestionOptionPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameQuestionOptionRepository extends JpaRepository<GameQuestionOptionPO, Long> {
    List<GameQuestionOptionPO> findByQuestionId(Long questionId);
    void deleteByQuestionId(Long questionId);
}
