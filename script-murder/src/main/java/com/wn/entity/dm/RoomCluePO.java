package com.wn.entity.dm;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @Author: 杜江
 * @Description:对局房间玩家已获取线索记录表
 * @DateTime: 2026/6/25 11:15
 * @Component:
 **/
@Data
@Entity
@Table(name = "dm_room_clue")
@DynamicInsert
@DynamicUpdate
public class RoomCluePO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;     ///主键 ID，自增，本条线索领取记录的唯一编号

    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;  ///房间 ID（UUID 格式，长度 36），用来区分不同对局

    @Column(name = "player_id", nullable = false)
    private Long playerId;  ///玩家 ID，记录是谁搜到了这条线索

    @Column(name = "clue_id", nullable = false)
    private Long clueId;  ///线索主键 ID，关联剧本基础线索表

    @Column(name = "obtain_time")
    private LocalDateTime obtainTime;  ///领取时间，记录玩家搜到这条线索的时间

    @Column(name = "is_public")
    private int isPublic = 0;  ///是否公开，0 表示不公开，1 表示公开 ，默认0

    @PrePersist
    public void prePersist() {
        this.obtainTime = LocalDateTime.now();
    }
}
