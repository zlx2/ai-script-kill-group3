/**
 * @Author: 鱼
 * @Description:WebSocket 处理器（核心）
 * @DateTime: 2026/6/25 10:27
 * @Component:
 **/
package com.wn.websocket;

import com.alibaba.fastjson2.JSON;
import com.wn.service.room.GameRoomService;
import com.wn.websocket.vo.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏 WebSocket 处理器
 *
 * 【核心职责】
 * 1. 管理所有 WebSocket 连接
 * 2. 管理大厅成员
 * 3. 管理各个房间的成员
 * 4. 提供广播方法
 *
 * 【数据结构说明】
 * 为什么用 ConcurrentHashMap？
 * - HashMap 不是线程安全的，多线程同时改会出问题
 * - ConcurrentHashMap 是线程安全的，适合并发场景
 * - WebSocket 连接是多线程的，所以必须用线程安全的集合
 */
@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private GameRoomService gameRoomService;

    @Autowired
    public void setGameRoomService(@Lazy GameRoomService gameRoomService) {  // ← 加这整个方法
        this.gameRoomService = gameRoomService;
    }

    // ==================== 数据存储 ====================

    /**
     * 所有用户的会话：userId → WebSocketSession
     *
     * 作用：根据用户ID找到他的连接，给他发消息
     *
     * 注意：这里简化为一个用户一个连接
     * 如果用户开多个标签页，后面的会覆盖前面的
     * 进阶版可以改成：userId → List<WebSocketSession>
     */
    private final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    /**
     * 大厅成员：userId → true
     *
     * 作用：记录哪些用户在大厅页面
     * 创建/解散房间时，给这些人发通知
     */
    private final ConcurrentHashMap<Long, Boolean> lobbyMembers = new ConcurrentHashMap<>();

    /**
     * 房间成员：roomId → (userId → true)
     *
     * 作用：记录每个房间里有哪些用户在线
     * 房间内有事件时，给房间里的所有人发通知
     *
     * 数据结构解释：
     * 外层 Map 的 key 是房间ID
     * 内层 Map 的 key 是用户ID，value 固定是 true（只需要存有没有，不需要存具体值）
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, Boolean>> roomMembers = new ConcurrentHashMap<>();

    // ==================== 1. 连接建立 ====================

    /**
     * 连接建立成功后自动调用
     *
     * 类比：有人进小区了，门卫给他登记一下，问他去几栋几单元
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 1. 从连接参数里取出 userId 和 roomId
        Long userId = getParamFromSession(session, "userId", Long.class);
        String roomId = getParamFromSession(session, "roomId", String.class);

        // 2. 参数校验
        if (userId == null) {
            log.warn("WebSocket 连接失败：缺少 userId");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // 3. 保存用户会话
        userSessions.put(userId, session);

        // 4. 判断是在大厅还是在房间
        if (roomId == null || "lobby".equals(roomId)) {
            // 在大厅
            lobbyMembers.put(userId, true);
            log.info("用户进入大厅：userId={}，当前大厅人数：{}", userId, lobbyMembers.size());
        } else {
            // 在房间
            roomMembers.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                    .put(userId, true);
            log.info("用户进入房间：userId={}, roomId={}，当前房间人数：{}",
                    userId, roomId, roomMembers.get(roomId).size());
        }
    }

    // ==================== 2. 连接关闭 ====================

    /**
     * 连接关闭后自动调用
     *
     * 类比：有人离开小区了，门卫把他的登记信息删掉
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getParamFromSession(session, "userId", Long.class);
        String roomId = getParamFromSession(session, "roomId", String.class);

        if (userId != null && roomId != null && !"lobby".equals(roomId)) {
            try {
                gameRoomService.forceLeaveRoom(roomId, userId);
                log.info("WebSocket断开，用户强制退出房间: userId={}, roomId={}", userId, roomId);
            } catch (Exception e) {
                log.error("用户断开连接强制退出房间失败: userId={}, roomId={}", userId, roomId, e);
            }
        }

        // 清理内存
        if (userId != null) {
            userSessions.remove(userId);
            lobbyMembers.remove(userId);
            if (roomId != null && !"lobby".equals(roomId)) {
                ConcurrentHashMap<Long, Boolean> members = roomMembers.get(roomId);
                if (members != null) {
                    members.remove(userId);
                    if (members.isEmpty()) {
                        roomMembers.remove(roomId);
                    }
                }
            }
            log.info("用户断开连接：userId={}", userId);
        }
    }

    // ==================== 3. 收到消息 ====================

    /**
     * 收到前端发来的消息时调用
     *
     * 类比：有人给物业打电话，说"帮我喊一下3栋的张三"
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到 WebSocket 消息：{}", payload);

        try {
            // 解析消息
            WsMessage<?> wsMessage = JSON.parseObject(payload, WsMessage.class);

            // 根据消息类型处理
            switch (wsMessage.getType()) {
                case "ping":
                    // 心跳：前端发 ping，后端回 pong
                    Long userId = getParamFromSession(session, "userId", Long.class);
                    if (userId != null) {
                        sendToUser(userId, WsMessage.builder()
                                .type("pong")
                                .timestamp(System.currentTimeMillis())
                                .build());
                    }
                    break;

                // TODO: 可以在这里加更多消息处理，比如聊天消息
                // case "chat":
                //     handleChatMessage(session, wsMessage);
                //     break;

                default:
                    log.warn("未知的消息类型：{}", wsMessage.getType());
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败", e);
        }
    }

    // ==================== 4. 广播方法（对外提供） ====================

    /**
     * 给大厅里的所有人广播
     *
     * 使用场景：
     * - 有人创建了新房间
     * - 有人解散了房间
     * - 房间状态更新（开始游戏了、人数变了）
     */
    public <T> void broadcastToLobby(WsMessage<T> message) {
        if (lobbyMembers.isEmpty()) {
            return;
        }

        String json = JSON.toJSONString(message);
        log.debug("广播给大厅：{}，人数：{}", message.getType(), lobbyMembers.size());

        for (Long userId : lobbyMembers.keySet()) {
            sendToUserInternal(userId, json);
        }
    }

    /**
     * 给指定房间里的所有人广播
     *
     * 使用场景：
     * - 玩家加入/离开房间
     * - 玩家准备/取消准备
     * - 游戏开始
     * - 聊天消息
     * - 阶段变更
     */
    public <T> void broadcastToRoom(String roomId, WsMessage<T> message) {
        ConcurrentHashMap<Long, Boolean> members = roomMembers.get(roomId);
        if (members == null || members.isEmpty()) {
            return;
        }

        String json = JSON.toJSONString(message);
        log.debug("广播给房间：{}，消息类型：{}，人数：{}", roomId, message.getType(), members.size());

        for (Long userId : members.keySet()) {
            sendToUserInternal(userId, json);
        }
    }

    /**
     * 给指定用户发消息
     *
     * 使用场景：
     * - 私聊
     * - 系统通知（"你被踢了"）
     * - 错误提示
     */
    public <T> void sendToUser(Long userId, WsMessage<T> message) {
        String json = JSON.toJSONString(message);
        sendToUserInternal(userId, json);
    }

    // ==================== 5. 频道切换方法 ====================

    /**
     * 用户从大厅进入房间
     *
     * 调用时机：用户点"加入房间"成功后调用
     */
    public void userEnterRoom(Long userId, String roomId) {
        // 从大厅移除
        lobbyMembers.remove(userId);

        // 加入房间
        roomMembers.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(userId, true);

        log.info("用户切换到房间：userId={}, roomId={}", userId, roomId);
    }

    /**
     * 用户从房间回到大厅
     *
     * 调用时机：用户退出房间后调用
     */
    public void userLeaveRoom(Long userId, String roomId) {
        // 从房间移除
        ConcurrentHashMap<Long, Boolean> members = roomMembers.get(roomId);
        if (members != null) {
            members.remove(userId);
            if (members.isEmpty()) {
                roomMembers.remove(roomId);
            }
        }

        // 加回大厅
        lobbyMembers.put(userId, true);

        log.info("用户回到大厅：userId={}, roomId={}", userId, roomId);
    }

    // ==================== 6. 查询方法 ====================

    /**
     * 获取大厅在线人数
     */
    public int getLobbyOnlineCount() {
        return lobbyMembers.size();
    }

    /**
     * 获取房间在线人数
     */
    public int getRoomOnlineCount(String roomId) {
        ConcurrentHashMap<Long, Boolean> members = roomMembers.get(roomId);
        return members != null ? members.size() : 0;
    }

    /**
     * 判断用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    // ==================== 7. 内部工具方法 ====================

    /**
     * 给指定用户发消息（内部方法，传 JSON 字符串）
     */
    private void sendToUserInternal(Long userId, String json) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("发送 WebSocket 消息失败：userId={}", userId, e);
            }
        }
    }

    /**
     * 从 WebSocket 连接参数里取出指定参数
     *
     * 前端连接时是这样的：ws://xxx/ws/game?userId=1001&roomId=lobby
     * 这个方法就是从 ? 后面的参数里取值
     */
    private <T> T getParamFromSession(WebSocketSession session, String paramName, Class<T> type) {
        // 1. 拿到查询字符串，比如 "userId=1001&roomId=lobby"
        String query = session.getUri().getQuery();
        if (query == null) {
            return null;
        }

        // 2. 按 & 分割，遍历每个参数
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length >= 2 && paramName.equals(pair[0])) {
                String value = pair[1];

                // 3. 根据类型转换
                if (type == Long.class) {
                    return type.cast(Long.parseLong(value));
                } else if (type == String.class) {
                    return type.cast(value);
                } else if (type == Integer.class) {
                    return type.cast(Integer.parseInt(value));
                }
            }
        }

        return null;
    }
}