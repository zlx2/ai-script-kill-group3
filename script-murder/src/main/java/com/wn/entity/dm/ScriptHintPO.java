package com.wn.entity.dm;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @Author: 杜江
 * @Description:dm扶车提示实体
 * @DateTime: 2026/6/25 11:07
 * @Component:
 **/
@Data
@Entity
@Table(name = "dm_script_hint")
@DynamicInsert
@DynamicUpdate
public class ScriptHintPO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hint_id")
    private Long hintId;     //提示id

    @Column(name = "script_id", nullable = false)
    private Long scriptId;     //剧本id

    @Column(name = "hint_level", nullable = false)
    private Byte hintLevel;     //提示等级

    @Column(name = "hint_content", columnDefinition = "TEXT")
    private String hintContent;     //提示内容

    @Column(name = "trigger_condition", columnDefinition = "TEXT")
    private String triggerCondition;     //触发条件

    @Column(name = "act_order")
    private Integer actOrder;       //执行顺序

    @Column(name = "target_role_id")
    private Long targetRoleId;       //目标角色id

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
