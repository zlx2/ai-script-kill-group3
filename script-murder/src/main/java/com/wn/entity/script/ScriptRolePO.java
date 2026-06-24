package com.wn.entity.script;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @Author: 杜江
 * @Description:剧本角色实体类
 * @DateTime: 2026/6/24 10:38
 * @Component:
 **/
@Data
@Entity
@Table(name = "script_role")
@DynamicInsert
@DynamicUpdate
public class ScriptRolePO {

    // 角色ID 自增主键
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    // 剧本ID
    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    // 角色名称
    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    // 性别
    @Column(name = "gender", length = 10)
    private String gender;

    //角色年龄
    @Column(name = "age")
    private Integer age;

    // 角色故事
    @Column(name = "character_story", columnDefinition = "TEXT")
    private String characterStory;

    // 角色秘密信息
    @Column(name = "secret_info", columnDefinition = "TEXT")
    private String secretInfo;

    // 创建时间
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
