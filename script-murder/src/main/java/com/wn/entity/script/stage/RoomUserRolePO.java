/**
 * @Author: 弗
 * @Description: 
 * @DateTime: 2026/6/25 11:08
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
@Table(name = "room_user_role")
@DynamicInsert
@DynamicUpdate
public class RoomUserRolePO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "room_id", nullable = false, length = 64)
    private String roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
