package com.wn.entity.script;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @Author: 杜江
 * @Description:剧本任务PO类
 * @DateTime: 2026/6/24 10:43
 * @Component:
 **/
@Data
@Entity
@Table(name = "ai_script_task")
@DynamicInsert
@DynamicUpdate
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiScriptTaskPO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "script_id")
    private Long scriptId;

    @Column(name = "task_status")
    private Integer taskStatus;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "error_msg", length = 500)
    private String errorMsg;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "script_theme", nullable = false, length = 100)
    private String scriptTheme;

    @Column(name = "script_type", nullable = false, length = 100)
    private String scriptType;

    @Column(name = "player_count", nullable = false, length = 100)
    private String playerCount;

    @Column(name = "difficulty", nullable = false, length = 50)
    private String difficulty;

    @Column(name = "background_desc", columnDefinition = "TEXT")
    private String backgroundDesc;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
