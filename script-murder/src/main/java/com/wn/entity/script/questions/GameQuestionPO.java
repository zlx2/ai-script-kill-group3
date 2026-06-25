/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/25 16:48
 * @Component:
 **/
package com.wn.entity.script.questions;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "game_question")
@DynamicInsert
@DynamicUpdate
public class GameQuestionPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_type", nullable = false)
    private String roleType;

    @Column(name = "question_title", nullable = false)
    private String questionTitle;

    @Column(name = "sort_num", nullable = false)
    private Integer sortNum;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    // 一对多：题目下所有选项
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "question_id")
    private List<GameQuestionOptionPO> optionList;

    // 一对一：标准答案解析
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id", referencedColumnName = "question_id")
    private GameQuestionAnalysisPO analysis;

    @PrePersist
    public void preSave() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
