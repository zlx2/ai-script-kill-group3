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
 * @Description:
 * @DateTime: 2026/6/24 12:11
 * @Component:
 **/
@Data
@Entity
@Table(name = "ai_script_outline")
@DynamicInsert
@DynamicUpdate
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiScriptOutlinePO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "theme", nullable = false, length = 100)
    private String theme;

    @Column(name = "outline", columnDefinition = "TEXT")
    private String outline;

    @Column(name = "background_story", columnDefinition = "TEXT")
    private String backgroundStory;

    @Column(name = "core_trick", columnDefinition = "TEXT")
    private String coreTrick;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
