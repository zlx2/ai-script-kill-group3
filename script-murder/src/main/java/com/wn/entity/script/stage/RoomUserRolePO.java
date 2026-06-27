/**
 * @Author: 弗
 * @Description: 房间用户角色绑定表
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
    //主键
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    //房间ID
    @Column(name = "room_id", nullable = false, length = 64)
    private String roomId;
    //用户ID
    @Column(name = "user_id", nullable = false)
    private Long userId;
    //剧本ID
    @Column(name = "script_id", nullable = false)
    private Long scriptId;
    //角色ID
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    //创建时间
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
