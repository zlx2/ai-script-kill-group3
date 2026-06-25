package com.wn.entity.dm;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @Author: 杜江
 * @Description:对局房间玩家投票记录表
 * @DateTime: 2026/6/25 11:19
 * @Component:
 **/
@Data
@Entity
@Table(name = "dm_room_vote")
@DynamicInsert
@DynamicUpdate
public class RoomVotePO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;     ///主键 ID，自增，本条投票记录的唯一编号

    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;  ///房间 ID（UUID 格式，长度 36），用来区分不同对局

    @Column(name = "player_id", nullable = false)
    private Long playerId;  ///玩家 ID，记录是谁投票了

    @Column(name = "vote_role_id", nullable = false)
    private Long voteRoleId;  ///被投票的角色 ID，代表玩家投给了谁

    @Column(name = "vote_time")
    private LocalDateTime voteTime;  ///投票时间，记录玩家投票的时间

    @Column(name = "is_correct")
    private Byte isCorrect;         ///是否正确，0 表示错误，1 表示投票命中真凶

    @PrePersist
    public void prePersist() {
        this.voteTime = LocalDateTime.now();
    }
}
