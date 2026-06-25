/**
 * @Author: 鱼
 * @Description:统一消息体
 * @DateTime: 2026/6/25 10:28
 * @Component:
 **/
package com.wn.websocket.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 统一消息体
 *
 * 【设计思想】
 * 所有 WebSocket 消息都用这个格式，前端根据 type 判断消息类型。
 *
 * 【type 取值说明】
 * 大厅相关：
 *   - new_room       有新房间创建
 *   - room_dismiss   有房间解散
 *   - room_update    房间信息更新（人数变了、开始游戏了）
 *
 * 房间相关：
 *   - player_join    玩家加入房间
 *   - player_leave   玩家离开房间
 *   - player_ready   玩家准备/取消准备
 *   - host_transfer  转让房主
 *   - game_start     游戏开始
 *   - chat           聊天消息
 *   - stage_change   游戏阶段变更
 *
 * 系统相关：
 *   - ping           心跳（前端发）
 *   - pong           心跳回复（后端发）
 *   - error          错误消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage<T> {

    /**
     * 消息类型（见上面的注释）
     */
    private String type;

    /**
     * 消息数据
     * 不同的 type，data 的结构不一样
     * 比如 type=player_join，data 里有 userId、userName
     * 比如 type=chat，data 里有 userId、content
     */
    private T data;

    /**
     * 时间戳（毫秒）
     */
    private Long timestamp;
}