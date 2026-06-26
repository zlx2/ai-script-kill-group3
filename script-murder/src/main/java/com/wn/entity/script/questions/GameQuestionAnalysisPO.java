/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 16:50
 * @Component:
 **/
package com.wn.entity.script.questions;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "game_question_analysis")
public class GameQuestionAnalysisPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "analysis", nullable = false, columnDefinition = "TEXT")
    private String analysis;
}
