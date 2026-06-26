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
@Table(name = "game_question_option")
public class GameQuestionOptionPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "option_code", nullable = false)
    private String optionCode;

    @Column(name = "option_content", nullable = false)
    private String optionContent;

    @Column(name = "is_correct")
    private Integer isCorrect;

    @Column(name = "score")
    private Integer score;
}
