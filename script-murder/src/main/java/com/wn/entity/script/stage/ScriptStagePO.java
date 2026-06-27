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
@Table(name = "script_stage")
@DynamicInsert
@DynamicUpdate
public class ScriptStagePO {
    // 分幕ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stage_id")
    private Long stageId;
    // 剧本ID
    @Column(name = "script_id", nullable = false)
    private Long scriptId;
    // 分幕编号
    @Column(name = "stage_no", nullable = false)
    private Integer stageNo;
    // 分幕名称
    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName;
    // 解锁阶段
    @Column(name = "unlock_stage",nullable = false, length = 32)
    private String unlockStage;
    // 创建时间
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
