/**
 * @Author: 弗
 * @Description: 
 * @DateTime: 2026/6/25 11:07
 * @Component: 
 **/
package com.wn.entity.script.stage;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "role_stage_content")
@DynamicInsert
@DynamicUpdate
public class RoleStageContentPO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @Column(name = "main_content", columnDefinition = "TEXT", nullable = false)
    private String mainContent;

    @Column(name = "hint_content", columnDefinition = "TEXT")
    private String hintContent;

    @Column(name = "unlock_stage", length = 32)
    private String unlockStage;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
