package com.wn.entity.clue;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @Author: 杜江
 * @Description: 脚本线索实体类
 * @DateTime: 2026/6/25 10:58
 * @Component:
 **/
@Data
@Entity
@Table(name = "dm_script_clue")
@DynamicInsert
@DynamicUpdate
public class ScriptCluePO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clue_id")
    private Long clueId;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "clue_name", nullable = false, length = 200)
    private String clueName;

    @Column(name = "clue_description", columnDefinition = "TEXT")
    private String clueDescription;

    @Column(name = "scene", length = 100)
    private String scene;

    @Column(name = "is_hidden")
    private Byte isHidden = 0;

    @Column(name = "unlock_condition", columnDefinition = "TEXT")
    private String unlockCondition;

    @Column(name = "act_order")
    private Integer actOrder;

    @Column(name = "importance_level")
    private Byte importanceLevel = 1;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
