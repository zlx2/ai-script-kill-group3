package com.wn.entity.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/24 17:36
 * @Component:
 **/


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePO {
    private Long roomId;
    private Long userId;
    private Long npcRoleId;
    private String npcRoleName;
    private String content;
    private String role;
    private LocalDateTime createTime;
}