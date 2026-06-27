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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    //================================================心跳监测（在线检测）=============================================================

    /** 心跳超时时间：60秒无心跳视为断开 */
    private static final long HEARTBEAT_TIMEOUT_MS = 60_000;

    /** 心跳检测间隔 */
    private static final long HEARTBEAT_CHECK_INTERVAL_MS = 30_000;

    /** 用户最后心跳时间：userId → 时间戳 */
    private final ConcurrentHashMap<Long, Long> userHeartbeats = new ConcurrentHashMap<>();

    /** 心跳检测定时器 */
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * 初始化心跳检测定时器
     */
    @PostConstruct
    public void init() {
        heartbeatScheduler.scheduleAtFixedRate(
                this::checkHeartbeat,
                HEARTBEAT_CHECK_INTERVAL_MS,
                HEARTBEAT_CHECK_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        log.info("WebSocket 心跳检测已启动，间隔={}ms", HEARTBEAT_CHECK_INTERVAL_MS);
    }

    /**
     * 销毁心跳检测定时器
     */
    @PreDestroy
    public void destroy() {
        heartbeatScheduler.shutdown();
        log.info("WebSocket 心跳检测已停止");
    }
    /**
     * 定期检查心跳，清理超时连接
     */
    private void checkHeartbeat() {
        long now = System.currentTimeMillis();
        userHeartbeats.forEach((userId, lastHeartbeat) -> {
            if (now - lastHeartbeat > HEARTBEAT_TIMEOUT_MS) {
                log.warn("心跳超时，清理连接: userId={}, 最后心跳={}ms前",
                        userId, now - lastHeartbeat);
                WebSocketSession session = userSessions.get(userId);
                if (session != null && session.isOpen()) {
                    try {
                        // 关闭连接
                        session.close(CloseStatus.SESSION_NOT_RELIABLE);
                    } catch (IOException e) {
                        log.debug("关闭超时连接异常: userId={}", userId, e);
                    }
                }
                // 清理用户数据
                cleanupUser(userId);
            }
        });
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

        /**
         * 3. 【修复】如果用户已存在（重连），先关闭旧连接并清理旧状态
         *  原因：当连接断开时，afterConnectionClosed 通过 session.getUri().getQuery() 读取 roomId。
         *      但用户通过 HTTP API 加入房间后，WebSocket 的 session URI 不会更新，所以读到的仍然是旧的 roomId
         *  解决：新增 findUserRoomId() 方法，遍历 roomMembers 查找用户实际所在的房间，不再依赖 session URI。
         */
        WebSocketSession oldSession = userSessions.get(userId);
        if (oldSession != null && oldSession.isOpen()) {
            log.info("用户重连，关闭旧连接: userId={}", userId);
            try {
                oldSession.close(CloseStatus.SESSION_NOT_RELIABLE);
            } catch (IOException e) {
                log.debug("关闭旧连接异常: userId={}", userId, e);
            }
        }
        cleanupUser(userId);

        // 4. 保存用户会话
        userSessions.put(userId, session);
        // 保存心跳时间
        userHeartbeats.put(userId, System.currentTimeMillis());

        // 5. 判断是在大厅还是在房间
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
     *      【修复内容】
     *      不再依赖 session URI 中的 roomId（该参数在建立连接后不会更新），
     *      改为遍历 roomMembers 查找用户实际所在的房间进行清理，
     *      避免因 session URI 记录的 roomId 与实际状态不一致导致用户"卡在"房间中。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getParamFromSession(session, "userId", Long.class);

        if (userId != null) {
            // 【修复】遍历 roomMembers 查找用户实际所在的房间
            String userRoomId = findUserRoomId(userId);
            if (userRoomId != null) {
                try {
                    gameRoomService.forceLeaveRoom(userRoomId, userId);
                    log.info("WebSocket断开，用户强制退出房间: userId={}, roomId={}", userId, userRoomId);
                } catch (Exception e) {
                    log.error("用户断开连接强制退出房间失败: userId={}, roomId={}", userId, userRoomId, e);
                }
            }
            // 统一清理所有内存记录
            cleanupUser(userId);
            log.info("用户断开连接：userId={}", userId);
        }
    }

    // ==================== 3. 收到消息 ====================

    /**
     * 收到前端发来的消息时调用
     *      【增添内容】
     *      添加了信令消息处理，确保在房间内进行的信令交换正常进行。
     *      发送当前状态给用户（用于重连后同步）
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
                /**
                 * 主叫端 创建 Offer → 发给服务端；
                 * 服务端转发 Offer → 给 被叫端；
                 * 被叫端 收到后创建 Answer → 发给服务端；
                 * 服务端转发 Answer → 给 主叫端；
                 * 双方同时开始交换各自的 Candidate（这个过程是并发的，可能穿插在上述步骤中），直到某一对 Candidate 成功打通“网络隧道”为止。
                 */
                case "webrtc_offer":// 主叫端 创建 Offer → 发给服务端；
                case "webrtc_answer":// 被叫端 收到后创建 Answer → 发给服务端；
                case "webrtc_candidate":// 双方同时开始交换各自的 Candidate（这个过程是并发的，可能穿插在上述步骤中），直到某一对 Candidate 成功打通“网络隧道”为止。
                    handleWebRtcSignal(session, wsMessage);
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
    /**
     * 【新增】发送当前状态给用户（用于重连后同步）
     */
    private void sendCurrentState(Long userId) {
        // 查找用户所在的房间
        // 如果用户不在大厅，返回空字符串
        String roomId = findUserRoomId(userId);
        boolean inLobby = lobbyMembers.containsKey(userId);

        // 构建状态同步数据
        Map<String, Object> syncData = new HashMap<>();
        syncData.put("inLobby", inLobby);
        syncData.put("roomId", roomId);
        syncData.put("serverTime", System.currentTimeMillis());

        // 发送状态同步消息
        sendToUser(userId, WsMessage.builder()
                .type("sync_state")
                .data(syncData)
                .timestamp(System.currentTimeMillis())
                .build());

        log.debug("已推送状态同步: userId={}, inLobby={}, roomId={}", userId, inLobby, roomId);
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
     * 【新增】统一清理用户在所有追踪结构中的记录
     * 在连接关闭、重连、心跳超时时调用，确保状态一致
     */
    private void cleanupUser(Long userId) {
        userSessions.remove(userId);
        lobbyMembers.remove(userId);
        userHeartbeats.remove(userId);

        // 遍历所有房间，清理该用户的记录
        roomMembers.forEach((roomId, members) -> {
            if (members != null) {
                members.remove(userId);
            }
        });
        // 移除空房间
        roomMembers.values().removeIf(Map::isEmpty);
    }

    /**
     * 【新增】遍历 roomMembers 查找用户所在的房间
     * 不依赖 session URI，始终返回用户当前实际所在的房间
     */
    private String findUserRoomId(Long userId) {
        for (Map.Entry<String, ConcurrentHashMap<Long, Boolean>> entry : roomMembers.entrySet()) {
            if (entry.getValue().containsKey(userId)) {
                return entry.getKey();
            }
        }
        return null;
    }



    /**
     * 给指定用户发消息（内部方法，传 JSON 字符串）
     *
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

    // =================================== WebRTC 信令转发 ====================================================
    @SuppressWarnings("unchecked")// 忽略类型检查警告，因为 data 是 Map<String, Object>
    private void handleWebRtcSignal(WebSocketSession session, WsMessage<?> wsMessage) {
        if (!(wsMessage.getData() instanceof Map)) return;

        Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
        Long sourceUserId = getParamFromSession(session, "userId", Long.class);
        Object targetObj = data.get("targetUserId");

        if (sourceUserId == null || targetObj == null) return;

        Long targetUserId = targetObj instanceof Long ? (Long) targetObj
                : Long.valueOf(targetObj.toString());

        data.put("sourceUserId", sourceUserId);

        WsMessage<Map<String, Object>> relay = WsMessage.<Map<String, Object>>builder()
                .type(wsMessage.getType())
                .data(data)
                .timestamp(System.currentTimeMillis())// 记录当前时间戳（毫秒）
                .build();

        sendToUser(targetUserId, relay);
        log.debug("WebRTC 信令: {} -> {}, type={}", sourceUserId, targetUserId, wsMessage.getType());
    }
}