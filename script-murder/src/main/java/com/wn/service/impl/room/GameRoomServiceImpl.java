package com.wn.service.impl.room;

import com.wn.controller.enums.room.GameStageEnum;
import com.wn.controller.enums.room.RoomStatusEnum;
import com.wn.controller.room.vo.RoomDetailVO;
import com.wn.controller.room.vo.RoomPlayerVO;
import com.wn.entity.room.RoomPO;
import com.wn.entity.room.RoomPlayerPO;
import com.wn.entity.script.ScriptPO;
import com.wn.entity.script.ScriptRolePO;
import com.wn.entity.script.stage.RoomUserRolePO;
import com.wn.entity.script.stage.ScriptStagePO;
import com.wn.entity.user.Userinfo;
import com.wn.mapper.room.RoomMapper;
import com.wn.mapper.room.RoomPlayerMapper;
import com.wn.mapper.script.RoomUserRoleMapper;
import com.wn.mapper.script.ScriptRoleMapper;
import com.wn.mapper.script.ScriptStageMapper;
import com.wn.service.auth.UserService;
import com.wn.service.exception.BusinessException;
import com.wn.service.room.GameRoomService;
import com.wn.service.script.ScriptService;
import com.wn.websocket.WebSocketHandler;
import com.wn.websocket.vo.WsMessage;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameRoomServiceImpl implements GameRoomService {

    private final RoomMapper roomMapper;
    private final RoomPlayerMapper roomPlayerMapper;
    private final ScriptService scriptService;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    private final WebSocketHandler webSocketHandler;

    @Resource
    private ScriptRoleMapper scriptRoleMapper;

    @Resource
    private RoomUserRoleMapper roomUserRoleMapper;

    @Resource
    private ScriptStageMapper scriptStageMapper;

    /**
     * 创建房间
     */
    @Override
    public String createRoom(Long scriptId, String roomName, String password, Long userId) throws BusinessException {
        // 校验是否有剧本
        ScriptPO script = scriptService.getById(scriptId);
        if (script == null || script.getStatus() != 1) {
            throw new BusinessException("剧本不存在或已下架");
        }
        // 2. 生成唯一房间号
        String roomNo = generateUniqueRoomNo();
        // 3. 创建房间
        RoomPO room = new RoomPO();
        room.setRoomName(roomName != null ? roomName : script.getScriptName());
        room.setRoomNo(roomNo);
        room.setScriptId(scriptId);
        room.setHostId(userId);
        room.setMaxPlayer(script.getPlayerCount());
        room.setCurrentPlayer(1);
        room.setRoomStatus(RoomStatusEnum.WAITING.getCode());
        room.setCurrentRound(0);
        room.setCurrentStage(GameStageEnum.WAITING.getCode());
        room.setPassword(password);
        roomMapper.save(room);
        // 4. 房主加入
        RoomPlayerPO player = new RoomPlayerPO();
        player.setRoomId(room.getRoomId());
        player.setUserId(userId);
        player.setIsReady((byte) 1);
        player.setIsHost((byte) 1);
        player.setJoinTime(LocalDateTime.now());
        roomPlayerMapper.save(player);
        // 5. 缓存
        cacheRoomInfo(room);
        //创建房间后广播给大厅
        broadcastNewRoom(room);
        // 【修复】将创建者从大厅切换到房间的 WebSocket 频道
        webSocketHandler.userEnterRoom(userId, room.getRoomId());
        return roomNo;
    }

    /**
     * 缓存房间信息
     */
    private void cacheRoomInfo(RoomPO room) {
        String key = "room:info:" + room.getRoomId();
        redisTemplate.opsForValue().set(key, room, 30, TimeUnit.MINUTES);
    }

    /**
     * 生成6位唯一房间号
     */
    private String generateUniqueRoomNo() {
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            int num = 100000 + random.nextInt(900000);
            String roomNo = String.valueOf(num);
            if (!roomMapper.existsByRoomNo(roomNo)) {
                return roomNo;
            }
        }
        return String.valueOf(System.currentTimeMillis() % 1000000);
    }

    /**
     * 获取列表
     */
    @Override
    public List<RoomDetailVO> getRoomList() {
        List<RoomPO> rooms = roomMapper.findAll();
        return rooms.stream()
                .filter(r -> r.getRoomStatus() != 2)
                .map(this::buildRoomDetailVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void forceLeaveRoom(String roomId, Long userId) {
        RoomPlayerPO player = roomPlayerMapper
                .findByRoomIdAndUserIdAndLeaveTimeIsNull(roomId, userId);
        if (player == null) {
            return;
        }

        // 直接退出，不校验任何业务规则
        player.setLeaveTime(LocalDateTime.now());
        roomPlayerMapper.save(player);

        RoomPO room = roomMapper.findById(roomId).orElse(null);
        if (room == null) {
            return;
        }

        // 更新房间人数
        room.setCurrentPlayer(Math.max(0, room.getCurrentPlayer() - 1));

        // 如果房主离开，转让给下一个玩家
        if (player.getIsHost() == 1) {
            RoomPlayerPO newHost = roomPlayerMapper
                    .findFirstByRoomIdAndLeaveTimeIsNullOrderByJoinTimeAsc(roomId);
            if (newHost != null) {
                newHost.setIsHost((byte) 1);
                roomPlayerMapper.save(newHost);
                room.setHostId(newHost.getUserId());
            }
        }

        // 如果房间没人了，标记结束
        if (room.getCurrentPlayer() == 0) {
            room.setRoomStatus(RoomStatusEnum.ENDED.getCode());
            room.setEndTime(LocalDateTime.now());
            roomMapper.save(room);
        } else {
            roomMapper.save(room);
        }

        cacheRoomInfo(room);
        broadcastPlayerLeave(roomId, userId);
        broadcastRoomUpdate(room);
        webSocketHandler.userLeaveRoom(userId, roomId);
    }

    /**
     * 根据房间号查房间
     */
    @Override
    public RoomPO getByRoomNo(String roomNo) {
        return roomMapper.findByRoomNo(roomNo).orElse(null);
    }

    /**
     * 获取房间详情
     */
    @Override
    public RoomDetailVO getRoomDetail(String roomId) throws BusinessException {
        RoomPO room = roomMapper.findById(roomId).orElse(null);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }
        return buildRoomDetailVO(room);
    }

    /**
     * 构建房间详情VO
     */
    private RoomDetailVO buildRoomDetailVO(RoomPO room) {
        RoomDetailVO vo = new RoomDetailVO();
        vo.setRoomId(room.getRoomId());
        vo.setRoomNo(room.getRoomNo());
        vo.setRoomName(room.getRoomName());
        vo.setRoomStatus(room.getRoomStatus() != null ? (int) room.getRoomStatus() : 0);
        vo.setCurrentStage(room.getCurrentStage());
        vo.setCurrentRound(room.getCurrentRound());
        vo.setScriptId(room.getScriptId());
        vo.setHostId(room.getHostId());
        vo.setHasPassword(room.getPassword() != null && !room.getPassword().isEmpty());
        ScriptPO script = scriptService.getById(room.getScriptId());
        if (script != null) {
            vo.setScriptName(script.getScriptName());
            vo.setScriptCover(script.getCoverImage());
            vo.setScriptType(script.getScriptType());
            vo.setPlayerCount(script.getPlayerCount());
        }
        Userinfo host = userService.getById(room.getHostId());
        if (host != null) {
            vo.setHostNickname(host.getNickname());
            vo.setHostAvatar(host.getAvatar());
        }
        List<RoomPlayerVO> players = getRoomPlayers(room.getRoomId());
        vo.setPlayers(players);
        vo.setCurrentPlayer(players != null ? players.size() : 0);
        return vo;
    }

    /**
     * 根据房间号获取详情
     */
    @Override
    public RoomDetailVO getRoomDetailByNo(String roomNo) throws BusinessException {
        RoomPO room = roomMapper.findByRoomNo(roomNo).orElse(null);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }
        return buildRoomDetailVO(room);
    }
    /**
     * 转让房主（对外）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferHost(String roomId, Long targetUserId, Long userId) throws BusinessException {
        RoomPO room = roomMapper.findById(roomId).orElse(null);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }

        if (!room.getHostId().equals(userId)) {
            throw new BusinessException("只有房主才能转让");
        }
        RoomPlayerPO target = roomPlayerMapper
                .findByRoomIdAndUserIdAndLeaveTimeIsNull(roomId, targetUserId);
        if (target == null) {
            throw new BusinessException("目标玩家不在房间内");
        }
        RoomPlayerPO oldHost = roomPlayerMapper
                .findByRoomIdAndUserIdAndLeaveTimeIsNull(roomId, userId);
        if (oldHost != null) {
            oldHost.setIsHost((byte) 0);
            roomPlayerMapper.save(oldHost);
        }

        target.setIsHost((byte) 1);
        roomPlayerMapper.save(target);

        room.setHostId(targetUserId);
        roomMapper.save(room);

        // 转让房主后广播
        broadcastHostTransfer(roomId, targetUserId);
    }

    /**
     * 踢人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickPlayer(String roomId, Long targetUserId, Long userId) throws BusinessException {
        RoomPO room = roomMapper.findById(roomId).orElse(null);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }

        if (!room.getHostId().equals(userId)) {
            throw new BusinessException("只有房主才能踢人");
        }

        if (targetUserId.equals(userId)) {
            throw new BusinessException("不能踢自己");
        }

        leaveRoom(roomId, targetUserId);

        // 给被踢的人发私聊通知
        sendKickNotification(targetUserId, roomId);
    }

    /**
     * 解散房间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dismissRoom(String roomId, Long userId) throws BusinessException {
        RoomPO room = roomMapper.findById(roomId).orElse(null);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }

        if (!room.getHostId().equals(userId)) {
            throw new BusinessException("只有房主才能解散房间");
        }

        room.setRoomStatus(RoomStatusEnum.ENDED.getCode());
        room.setEndTime(LocalDateTime.now());
        roomMapper.save(room);

        List<RoomPlayerPO> players = roomPlayerMapper
                .findByRoomIdAndLeaveTimeIsNull(roomId);

        for (RoomPlayerPO player : players) {
            player.setLeaveTime(LocalDateTime.now());
            roomPlayerMapper.save(player);
        }

        // 解散房间后广播
        // 广播给房间内的人
        broadcastRoomDismiss(roomId);
        // 广播给大厅
        broadcastRoomDismissToLobby(roomId);
    }

    /**
     * 开始游戏
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startGame(String roomId, Long userId) throws BusinessException {
        RoomPO room = roomMapper.findById(roomId).orElse(null);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }

        if (!room.getHostId().equals(userId)) {
            throw new BusinessException("只有房主才能开始游戏");
        }

        if (!RoomStatusEnum.WAITING.getCode().equals(room.getRoomStatus())) {
            throw new BusinessException("当前状态无法开始游戏");
        }

        if (room.getCurrentPlayer() < 2) {
            throw new BusinessException("至少需要2名玩家才能开始");
        }

        long readyCount = roomPlayerMapper.countByRoomIdAndIsReadyAndLeaveTimeIsNull(
                roomId, (byte) 1
        );
        if (readyCount < room.getCurrentPlayer()) {
            throw new BusinessException("还有玩家未准备");
        }

        room.setRoomStatus(RoomStatusEnum.PLAYING.getCode());
        room.setCurrentStage("selecting");
        room.setCurrentRound(1);
        room.setStartTime(LocalDateTime.now());
        roomMapper.save(room);
        cacheRoomInfo(room);

        // 广播给房间内的人：游戏开始
        broadcastGameStart(roomId);
        // 广播给大厅：房间状态变了
        broadcastRoomUpdate(room);
        //新增：广播选角色阶段开始
        broadcastStageChange(roomId, "selecting");

        System.out.println("游戏已开始，房间ID：" + roomId + "，状态：" + room.getRoomStatus());
    }

    // 新增广播方法
    private void broadcastStageChange(String roomId, String stage) {
        Map<String, Object> data = new HashMap<>();
        data.put("stage", stage);

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("stage_change")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
        webSocketHandler.broadcastToRoom(roomId, message);
    }

    @Override
    public List<ScriptRolePO> getAvailableRoles(String roomId) {
        RoomPO room = roomMapper.findById(roomId).orElseThrow(
                () -> new BusinessException("房间不存在")
        );

        List<ScriptRolePO> allRoles = scriptRoleMapper.findByScriptId(room.getScriptId());
        List<RoomPlayerPO> players = roomPlayerMapper.findByRoomIdAndLeaveTimeIsNull(roomId);

        Set<Long> takenRoleIds = players.stream()
                .filter(p -> p.getRoleId() != null)
                .map(RoomPlayerPO::getRoleId)
                .collect(Collectors.toSet());

        return allRoles.stream()
                .filter(r -> !takenRoleIds.contains(r.getRoleId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void selectRole(String roomId, Long roleId, Long userId) {
        RoomPO room = roomMapper.findById(roomId).orElseThrow(
                () -> new BusinessException("房间不存在")
        );

        if (!"selecting".equals(room.getCurrentStage())) {
            throw new BusinessException("当前不是选角色阶段");
        }

        RoomPlayerPO player = roomPlayerMapper.findByRoomIdAndUserIdAndLeaveTimeIsNull(roomId, userId);
        if (player == null) {
            throw new BusinessException("玩家不在房间内");
        }

        if (player.getRoleId() != null) {
            throw new BusinessException("已选择角色，不能更改");
        }

        // 检查角色是否已被选
        List<RoomPlayerPO> players = roomPlayerMapper.findByRoomIdAndLeaveTimeIsNull(roomId);
        boolean taken = players.stream()
                .filter(p -> !p.getUserId().equals(userId))
                .anyMatch(p -> roleId.equals(p.getRoleId()));
        if (taken) {
            throw new BusinessException("该角色已被选择");
        }

        // 分配角色
        player.setRoleId(roleId);
        roomPlayerMapper.save(player);

        // 保存到 RoomUserRolePO
        RoomUserRolePO rur = new RoomUserRolePO();
        rur.setRoomId(roomId);
        rur.setUserId(userId);
        rur.setScriptId(room.getScriptId());
        rur.setRoleId(roleId);
        roomUserRoleMapper.save(rur);

        // 广播角色选择
        broadcastRoleSelected(roomId, userId, roleId);

        // 检查是否全部选完
        List<RoomPlayerPO> allPlayers = roomPlayerMapper.findByRoomIdAndLeaveTimeIsNull(roomId);
        boolean allSelected = allPlayers.stream().allMatch(p -> p.getRoleId() != null);

        if (allSelected) {
            room.setCurrentStage("reading");
            roomMapper.save(room);
            cacheRoomInfo(room);
            broadcastStageChange(roomId, "reading");
        }
    }

    @Override
    @Transactional
    public int advanceAct(String roomId) {
        RoomPO room = roomMapper.findById(roomId)
                .orElseThrow(() -> new BusinessException("房间不存在"));

        // 获取剧本所有分幕
        List<ScriptStagePO> stages = scriptStageMapper.findByScriptIdOrderByStageNoAsc(room.getScriptId());
        if (stages.isEmpty()) {
            throw new BusinessException("该剧本没有分幕");
        }

        int currentAct = room.getCurrentRound() == null ? 0 : room.getCurrentRound();
        if (currentAct >= stages.size()) {
            throw new BusinessException("已经是最后一幕");
        }

        // 推进到下一幕
        int newAct = currentAct + 1;
        room.setCurrentRound(newAct);

        // 更新当前阶段为下一幕对应的阶段
        ScriptStagePO nextStage = stages.get(newAct - 1);
        room.setCurrentStage(nextStage.getUnlockStage());

        roomMapper.save(room);
        cacheRoomInfo(room);

        // 广播阶段变更
        broadcastStageChange(roomId, nextStage.getUnlockStage());

        return newAct;
    }

    private void broadcastRoleSelected(String roomId, Long userId, Long roleId) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("roleId", roleId);

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("role_selected")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
        webSocketHandler.broadcastToRoom(roomId, message);
    }

    /**
     * 获取房间玩家列表
     */
    @Override
    public List<RoomPlayerVO> getRoomPlayers(String roomId) {
        List<RoomPlayerPO> players = roomPlayerMapper
                .findByRoomIdAndLeaveTimeIsNullOrderByJoinTimeAsc(roomId);

        if (players.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = players.stream()
                .map(RoomPlayerPO::getUserId)
                .collect(Collectors.toList());

        List<Userinfo> users = userService.listByIds(userIds);

        return players.stream().map(p -> {
            RoomPlayerVO vo = new RoomPlayerVO();
            vo.setUserId(p.getUserId());
            vo.setIsReady((int) p.getIsReady());
            vo.setIsHost((int) p.getIsHost());
            vo.setRoleId(p.getRoleId());

            users.stream()
                    .filter(u -> u.getUserId().equals(p.getUserId()))
                    .findFirst()
                    .ifPresent(u -> {
                        vo.setNickname(u.getNickname());
                        vo.setAvatar(u.getAvatar());
                    });

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 切换准备状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleReady(String roomId, Long userId) throws BusinessException {
        RoomPlayerPO player = roomPlayerMapper
                .findByRoomIdAndUserIdAndLeaveTimeIsNull(roomId, userId);

        if (player == null) {
            throw new BusinessException("玩家不在房间内");
        }

        player.setIsReady(player.getIsReady() == 1 ? (byte) 0 : (byte) 1);
        roomPlayerMapper.save(player);

        // 切换准备后广播
        broadcastPlayerReady(roomId, userId, player.getIsReady());
    }

    /**
     * 离开房间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveRoom(String roomId, Long userId) {
        RoomPlayerPO player = roomPlayerMapper
                .findByRoomIdAndUserIdAndLeaveTimeIsNull(roomId, userId);

        if (player == null) {
            return;
        }

        player.setLeaveTime(LocalDateTime.now());
        roomPlayerMapper.save(player);

        RoomPO room = roomMapper.findById(roomId).orElse(null);
        if (room != null) {
            room.setCurrentPlayer(Math.max(0, room.getCurrentPlayer() - 1));
            roomMapper.save(room);

            if (player.getIsHost() == 1) {
                transferHostInternal(roomId);
            }

            if (room.getCurrentPlayer() == 0) {
                room.setRoomStatus(RoomStatusEnum.ENDED.getCode());
                room.setEndTime(LocalDateTime.now());
                roomMapper.save(room);
            }

            cacheRoomInfo(room);

            //  WebSocket 修改8：离开房间后广播
            // 广播给房间内的人
            broadcastPlayerLeave(roomId, userId);
            // 广播给大厅（人数变了）
            broadcastRoomUpdate(room);
            // 切换 WebSocket 频道（从房间回到大厅）
            webSocketHandler.userLeaveRoom(userId, roomId);
        }
    }

    /**
     * 转让房主（内部，房主离开时自动转让）
     */
    private void transferHostInternal(String roomId) {
        RoomPlayerPO newHost = roomPlayerMapper
                .findFirstByRoomIdAndLeaveTimeIsNullOrderByJoinTimeAsc(roomId);

        if (newHost != null) {
            newHost.setIsHost((byte) 1);
            roomPlayerMapper.save(newHost);

            RoomPO room = roomMapper.findById(roomId).orElse(null);
            if (room != null) {
                room.setHostId(newHost.getUserId());
                roomMapper.save(room);

                //  自动转让房主后广播
                broadcastHostTransfer(roomId, newHost.getUserId());
            }
        }
    }

    /**
     * 加入房间
     */
    @Override
    public String joinRoom(String roomNo, String password, Long userId) throws BusinessException {
        RoomPO room = roomMapper.findByRoomNo(roomNo).orElse(null);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }

        if (RoomStatusEnum.PLAYING.getCode().equals(room.getRoomStatus())) {
            throw new BusinessException("游戏已开始，无法加入");
        }
        if (RoomStatusEnum.ENDED.getCode().equals(room.getRoomStatus())) {
            throw new BusinessException("房间已结束");
        }

        if (room.getPassword() != null && !room.getPassword().isEmpty()) {
            if (!room.getPassword().equals(password)) {
                throw new BusinessException("房间密码错误");
            }
        }

        String lockKey = "lock:room:join:" + room.getRoomId();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (locked == null || !locked) {
            throw new BusinessException("加入人数过多，请稍后重试");
        }

        try {
            room = roomMapper.findById(room.getRoomId()).orElse(null);
            if (room == null) {
                throw new BusinessException("房间不存在");
            }

            if (room.getCurrentPlayer() >= room.getMaxPlayer()) {
                throw new BusinessException("房间已满");
            }

            boolean exists = roomPlayerMapper.existsByRoomIdAndUserIdAndLeaveTimeIsNull(
                    room.getRoomId(), userId
            );
            if (exists) {
                return room.getRoomId();
            }

            RoomPlayerPO player = new RoomPlayerPO();
            player.setRoomId(room.getRoomId());
            player.setUserId(userId);
            player.setIsReady((byte) 0);
            player.setIsHost((byte) 0);
            player.setJoinTime(LocalDateTime.now());
            roomPlayerMapper.save(player);

            room.setCurrentPlayer(room.getCurrentPlayer() + 1);
            roomMapper.save(room);

            cacheRoomInfo(room);

            // 加入房间后广播
            // 广播给房间内的人
            broadcastPlayerJoin(room.getRoomId(), userId);
            // 广播给大厅（人数变了）
            broadcastRoomUpdate(room);
            // 切换 WebSocket 频道（从大厅进入房间）
            webSocketHandler.userEnterRoom(userId, room.getRoomId());

            return room.getRoomId();

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public RoomPO getByRoomId(String roomId) {
        return roomMapper.findById(roomId).orElse(null);
    }

    @Override
    public RoomPO findByRoomNo(String roomNo) throws BusinessException {
        RoomPO room = roomMapper.findByRoomNo(roomNo).orElse(null);
        if (room == null) {
            throw new BusinessException("房间号[" + roomNo + "]不存在");
        }
        return room;
    }



    /**
     * 广播：有新房间创建（给大厅）
     */
    private void broadcastNewRoom(RoomPO room) {
        Map<String, Object> roomInfo = buildRoomInfoForWs(room);

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("new_room")
                .data(roomInfo)
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketHandler.broadcastToLobby(message);
    }

    /**
     * 广播：房间信息更新（给大厅，比如人数变了、开始游戏了）
     */
    private void broadcastRoomUpdate(RoomPO room) {
        Map<String, Object> roomInfo = new HashMap<>();
        roomInfo.put("roomId", room.getRoomId());
        roomInfo.put("currentPlayer", room.getCurrentPlayer());
        roomInfo.put("roomStatus", room.getRoomStatus());

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("room_update")
                .data(roomInfo)
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketHandler.broadcastToLobby(message);
    }

    /**
     * 广播：房间解散（给大厅）
     */
    private void broadcastRoomDismissToLobby(String roomId) {
        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("room_dismiss")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketHandler.broadcastToLobby(message);
    }

    /**
     * 广播：玩家加入房间（给房间内所有人）
     */
    private void broadcastPlayerJoin(String roomId, Long userId) {
        Map<String, Object> data = buildPlayerInfoForWs(userId);

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("player_join")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketHandler.broadcastToRoom(roomId, message);
    }

    /**
     * 广播：玩家离开房间（给房间内所有人）
     */
    private void broadcastPlayerLeave(String roomId, Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("player_leave")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketHandler.broadcastToRoom(roomId, message);
    }

    /**
     * 广播：玩家准备/取消准备（给房间内所有人）
     */
    private void broadcastPlayerReady(String roomId, Long userId, Byte isReady) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("isReady", isReady != null ? isReady.intValue() : 0);

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("player_ready")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketHandler.broadcastToRoom(roomId, message);
    }

    /**
     * 广播：转让房主（给房间内所有人）
     */
    private void broadcastHostTransfer(String roomId, Long newHostId) {
        Map<String, Object> data = new HashMap<>();
        data.put("newHostId", newHostId);

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("host_transfer")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketHandler.broadcastToRoom(roomId, message);
    }

    /**
     * 广播：游戏开始（给房间内所有人）
     */
    private void broadcastGameStart(String roomId) {
        Map<String, Object> data = new HashMap<>();
        data.put("startTime", System.currentTimeMillis());

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("game_start")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketHandler.broadcastToRoom(roomId, message);
    }

    /**
     * 广播：房间解散（给房间内所有人）
     */
    private void broadcastRoomDismiss(String roomId) {
        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("reason", "dismiss");

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("room_dismiss")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketHandler.broadcastToRoom(roomId, message);
    }

    /**
     * 私聊：给被踢的人发通知
     */
    private void sendKickNotification(Long userId, String roomId) {
        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("reason", "kicked");
        data.put("message", "你被房主踢出了房间");

        WsMessage<Map<String, Object>> message = WsMessage.<Map<String, Object>>builder()
                .type("system_notice")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketHandler.sendToUser(userId, message);
    }


    /**
     * 构建房间信息（给 WebSocket 广播用）
     * 注意：不要传密码等敏感信息
     */
    private Map<String, Object> buildRoomInfoForWs(RoomPO room) {
        Map<String, Object> info = new HashMap<>();
        info.put("roomId", room.getRoomId());
        info.put("roomNo", room.getRoomNo());
        info.put("roomName", room.getRoomName());
        info.put("hostId", room.getHostId());
        info.put("currentPlayer", room.getCurrentPlayer());
        info.put("maxPlayer", room.getMaxPlayer());
        info.put("roomStatus", room.getRoomStatus());
        info.put("hasPassword", room.getPassword() != null && !room.getPassword().isEmpty());
        info.put("scriptId", room.getScriptId());

        // 可以加上剧本名、房主昵称等，看前端需要什么
        ScriptPO script = scriptService.getById(room.getScriptId());
        if (script != null) {
            info.put("scriptName", script.getScriptName());
            info.put("scriptType", script.getScriptType());
        }

        Userinfo host = userService.getById(room.getHostId());
        if (host != null) {
            info.put("hostNickname", host.getNickname());
            info.put("hostAvatar", host.getAvatar());
        }
        info.put("isReady", 0);
        info.put("isHost", 0);

        return info;
    }

    /**
     * 构建玩家信息（给 WebSocket 广播用）
     */
    private Map<String, Object> buildPlayerInfoForWs(Long userId) {
        Map<String, Object> info = new HashMap<>();
        info.put("userId", userId);

        Userinfo user = userService.getById(userId);
        if (user != null) {
            info.put("nickname", user.getNickname());
            info.put("avatar", user.getAvatar());
        }

        return info;
    }
}