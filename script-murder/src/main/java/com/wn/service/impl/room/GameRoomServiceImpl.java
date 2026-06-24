package com.wn.service.impl.room;

import com.wn.controller.enums.room.GameStageEnum;
import com.wn.controller.enums.room.RoomStatusEnum;
import com.wn.controller.room.vo.RoomDetailVO;
import com.wn.controller.room.vo.RoomPlayerVO;
import com.wn.entity.R;
import com.wn.entity.room.RoomPO;
import com.wn.entity.room.RoomPlayerPO;
import com.wn.entity.script.ScriptPO;
import com.wn.entity.user.Userinfo;
import com.wn.mapper.room.RoomMapper;
import com.wn.mapper.room.RoomPlayerMapper;
import com.wn.service.auth.UserService;
import com.wn.service.exception.BusinessException;
import com.wn.service.room.GameRoomService;
import com.wn.service.script.ScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
        // UUID和时间会在@PrePersist自动生成

        roomMapper.save(room);

        // 4. 房主加入
        RoomPlayerPO player = new RoomPlayerPO();
        player.setRoomId(room.getRoomId());
        player.setUserId(userId);
        player.setIsReady((byte) 1);
        player.setIsHost((byte) 1);
        player.setJoinTime(LocalDateTime.now());
        roomPlayerMapper.save(player);

        // 5. 缓存,MySQL 数据库插入房间成功后，立刻同步一份副本到 Redis。
        cacheRoomInfo(room);

        return roomNo;

    }

    /**
     * 缓存房间信息
     * 作用：自动清理无效房间
     * 房间结束、解散后不会主动删缓存，30 分钟无人访问自动淘汰，避免 Redis 堆积大量废弃房间数据；
     * 缓存自动兜底更新
     * 缓存过期后，下次查询房间会查 MySQL，再重新写入最新数据，解决缓存与数据库数据不一致问题；
     * 控制 Redis 内存占用，长期不用的数据自动释放。
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
            // 查询数据库：判断该房间号是否已存在
            if (!roomMapper.existsByRoomNo(roomNo)) {
                return roomNo;
            }
        }
        // 循环10次全部冲突（极端情况：10个随机号全在库内），走兜底方案
        // 当前时间毫秒对1000000取模，结果一定是0~999999的6位数字
        return String.valueOf(System.currentTimeMillis() % 1000000);
    }

//获取列表
    @Override
    public List<RoomDetailVO> getRoomList() {
        List<RoomPO> rooms = roomMapper.findAll();
        return rooms.stream()
                .filter(r -> r.getRoomStatus() != 2)
                .map(this::buildRoomDetailVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据房间号查房间
     */
    @Override
    public Object getByRoomNo(String roomNo) {

        return roomMapper.getByRoomNo(roomNo);
    }
    /**
     * 获取房间详情:整合完房间基础信息、剧本、房主、玩家、密码标记后，把完整 VO 返回给 Controller，直接序列化 JSON 给到前端页面。
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

        // ✅ 手动设置所有字段（不依赖 BeanUtils.copyProperties）
        vo.setRoomId(room.getRoomId());
        vo.setRoomNo(room.getRoomNo());
        vo.setRoomName(room.getRoomName());
        vo.setRoomStatus(room.getRoomStatus() != null ? (int) room.getRoomStatus() : 0);
        vo.setCurrentStage(room.getCurrentStage());
        vo.setCurrentRound(room.getCurrentRound());
        vo.setScriptId(room.getScriptId());
        vo.setHostId(room.getHostId());
        vo.setHasPassword(room.getPassword() != null && !room.getPassword().isEmpty());

        // 查询剧本信息
        ScriptPO script = scriptService.getById(room.getScriptId());
        if (script != null) {
            vo.setScriptName(script.getScriptName());
            vo.setScriptCover(script.getCoverImage());
            vo.setScriptType(script.getScriptType());
            vo.setPlayerCount(script.getPlayerCount());
        }

        // 查询房主用户信息
        Userinfo host = userService.getById(room.getHostId());
        if (host != null) {
            vo.setHostNickname(host.getNickname());
            vo.setHostAvatar(host.getAvatar());
        }

        // 查询玩家列表
        List<RoomPlayerVO> players = getRoomPlayers(room.getRoomId());
        vo.setPlayers(players);

        // ✅ 用实际玩家数量修正 currentPlayer
        vo.setCurrentPlayer(players != null ? players.size() : 0);

        return vo;
    }
    /**
     * 根据房间号获取详情
     */
    @Override
    public RoomDetailVO getRoomDetailByNo(String roomNo) throws BusinessException {
        RoomPO room = roomMapper.findByRoomNo(roomNo);
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

        // 旧房主取消
        RoomPlayerPO oldHost = roomPlayerMapper
                .findByRoomIdAndUserIdAndLeaveTimeIsNull(roomId, userId);
        if (oldHost != null) {
            oldHost.setIsHost((byte) 0);
            roomPlayerMapper.save(oldHost);
        }

        // 新房主
        target.setIsHost((byte) 1);
        roomPlayerMapper.save(target);

        room.setHostId(targetUserId);
        roomMapper.save(room);
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

        // 标记所有玩家离开
        List<RoomPlayerPO> players = roomPlayerMapper
                .findByRoomIdAndLeaveTimeIsNull(roomId);

        for (RoomPlayerPO player : players) {
            player.setLeaveTime(LocalDateTime.now());
            roomPlayerMapper.save(player);
        }
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

        // ✅ 更新房间状态
        room.setRoomStatus(RoomStatusEnum.PLAYING.getCode());  // 1
        room.setCurrentStage(GameStageEnum.READING.getCode());
        room.setCurrentRound(1);
        room.setStartTime(LocalDateTime.now());

        // ✅ 保存到数据库
        roomMapper.save(room);

        // ✅ 更新缓存
        cacheRoomInfo(room);

        System.out.println("游戏已开始，房间ID：" + roomId + "，状态：" + room.getRoomStatus());
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
                    .filter(u -> u.getId().equals(p.getUserId()))
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

        // 标记离开
        player.setLeaveTime(LocalDateTime.now());
        roomPlayerMapper.save(player);

        // 更新人数
        RoomPO room = roomMapper.findById(roomId).orElse(null);
        if (room != null) {
            room.setCurrentPlayer(Math.max(0, room.getCurrentPlayer() - 1));
            roomMapper.save(room);

            // 房主离开则转让
            if (player.getIsHost() == 1) {
                transferHostInternal(roomId);
            }

            // 没人了自动解散
            if (room.getCurrentPlayer() == 0) {
                room.setRoomStatus(RoomStatusEnum.ENDED.getCode());
                room.setEndTime(LocalDateTime.now());
                roomMapper.save(room);
            }

            cacheRoomInfo(room);
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
            }
        }
    }

    /**
     * 加入房间
     */
    @Override
    public String joinRoom(String roomNo, String password, Long userId) throws BusinessException {
        RoomPO room = roomMapper.findByRoomNo(roomNo);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }

        // 校验状态
        if (RoomStatusEnum.PLAYING.getCode().equals(room.getRoomStatus())) {
            throw new BusinessException("游戏已开始，无法加入");
        }
        if (RoomStatusEnum.ENDED.getCode().equals(room.getRoomStatus())) {
            throw new BusinessException("房间已结束");
        }

        // 校验密码
        if (room.getPassword() != null && !room.getPassword().isEmpty()) {
            if (!room.getPassword().equals(password)) {
                throw new BusinessException("房间密码错误");
            }
        }

        // 分布式锁防超员
        String lockKey = "lock:room:join:" + room.getRoomId();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (locked == null || !locked) {
            throw new BusinessException("加入人数过多，请稍后重试");
        }

        try {
            // 重新查
            room = roomMapper.findById(room.getRoomId()).orElse(null);
            if (room == null) {
                throw new BusinessException("房间不存在");
            }

            // 校验人数
            if (room.getCurrentPlayer() >= room.getMaxPlayer()) {
                throw new BusinessException("房间已满");
            }

            // 检查是否已在房间
            boolean exists = roomPlayerMapper.existsByRoomIdAndUserIdAndLeaveTimeIsNull(
                    room.getRoomId(), userId
            );
            if (exists) {
                return room.getRoomId();
            }

            // 加入
            RoomPlayerPO player = new RoomPlayerPO();
            player.setRoomId(room.getRoomId());
            player.setUserId(userId);
            player.setIsReady((byte) 0);
            player.setIsHost((byte) 0);
            player.setJoinTime(LocalDateTime.now());
            roomPlayerMapper.save(player);

            // 更新人数
            room.setCurrentPlayer(room.getCurrentPlayer() + 1);
            roomMapper.save(room);

            cacheRoomInfo(room);

            return room.getRoomId();

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public Object getByRoomId(Long roomId) {
        return null;
    }
}