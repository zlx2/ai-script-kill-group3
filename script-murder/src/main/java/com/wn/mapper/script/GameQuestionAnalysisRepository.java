/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 17:31
 * @Component:
 **/
package com.wn.mapper.script;

import com.wn.entity.script.questions.GameQuestionAnalysisPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameQuestionAnalysisRepository extends JpaRepository<GameQuestionAnalysisPO, Long> {
    Optional<GameQuestionAnalysisPO> findByQuestionId(Long questionId);
    void deleteByQuestionId(Long questionId);
}
