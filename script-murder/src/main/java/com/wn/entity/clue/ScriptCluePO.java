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

    //剧本id
    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    //线索名称
    @Column(name = "clue_name", nullable = false, length = 200)
    private String clueName;

    //线索描述
    @Column(name = "clue_description", columnDefinition = "TEXT")
    private String clueDescription;

    //线索所在场景
    @Column(name = "scene", length = 100)
    private int scene;//场景id，1表示第一幕，2表示第二幕，3表示第三幕...

    //是否隐藏 0隐藏 1公开
    @Column(name = "is_hidden")
    private int isHidden = 0;

    //解锁条件
    @Column(name = "unlock_condition", columnDefinition = "TEXT")
    private String unlockCondition;

    //线索顺序
    @Column(name = "act_order")
    private Integer actOrder;

    //线索重要等级 1普通 2重要 3紧急
    @Column(name = "importance_level")
    private Byte importanceLevel = 1;

    //创建时间
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 关联的角色ID（对应 ScriptRolePO.roleId），用于指定线索的所属角色
     */
    @Column(name = "role_id")
    private Long roleId;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
