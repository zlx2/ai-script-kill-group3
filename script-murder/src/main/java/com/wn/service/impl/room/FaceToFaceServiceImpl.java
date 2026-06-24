/**
 * @Author: 鱼
 * @Description:面对面建房服务
 * @DateTime: 2026/6/23 17:41
 * @Component:
 **/
package com.wn.service.impl.room;

import com.wn.controller.enums.room.RoomStatusEnum;
import com.wn.entity.room.RoomPO;
import com.wn.entity.room.RoomPlayerPO;
import com.wn.mapper.room.RoomMapper;
import com.wn.mapper.room.RoomPlayerMapper;
import com.wn.service.exception.BusinessException;
import com.wn.service.room.FaceToFaceService;
import com.wn.service.room.GameRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FaceToFaceServiceImpl implements FaceToFaceService {
    private final RoomMapper roomMapper;
    private final RoomPlayerMapper roomPlayerMapper;
    private final GameRoomService gameRoomService;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String F2F_KEY_PREFIX = "face2face:";
    private static final int EXPIRE_MINUTES = 5;
    /**
     * 创建面对面房间
     */
    @Override
    public String createFaceToFace(Long userId) {
        String code = generateUniqueCode();

        String key = F2F_KEY_PREFIX + code;
        redisTemplate.opsForHash().put(key, "hostId", userId);
        redisTemplate.opsForHash().put(key, "roomId", "null");
        redisTemplate.opsForHash().put(key, "createTime", System.currentTimeMillis());
        redisTemplate.expire(key, EXPIRE_MINUTES, TimeUnit.MINUTES);

        return code;
    }
    /**
     * 生成唯一房间码
     */
    private String generateUniqueCode() {
        Random random = new Random();
        int maxAttempts = 20;

        for (int i = 0; i < maxAttempts; i++) {
            int num = 1000 + random.nextInt(9000);
            String code = String.valueOf(num);

            String key = F2F_KEY_PREFIX + code;
            Boolean exists = redisTemplate.hasKey(key);

            if (exists == null || !exists) {
                return code;
            }
        }

        return String.valueOf(System.currentTimeMillis() % 10000);
    }
    /**
     * 加入面对面房间
     *
     * @param code   4位面对面号码
     * @param userId 用户ID
     * @return 房间ID（UUID字符串）
     * @throws BusinessException 业务异常
     */
    @Override
    public String joinFaceToFace(String code, Long userId) throws BusinessException {
        String key = F2F_KEY_PREFIX + code;

        // 1. 检查面对面号码是否存在
        Boolean exists = redisTemplate.hasKey(key);
        if (exists == null || !exists) {
            throw new BusinessException("面对面房间不存在或已过期");
        }

        // 2. 取出 roomId 字段
        Object roomIdObj = redisTemplate.opsForHash().get(key, "roomId");

        // ========== 第一个人加入：懒加载创建正式房间 ==========
        if (roomIdObj == null || "null".equals(roomIdObj.toString())) {

            // createRoom 返回的是 6位房间号（roomNo），不是 roomId！
            String roomNo = gameRoomService.createRoom(
                    1L,                    // 剧本ID（简化，实际项目中先进房再选）
                    "面对面房间",           // 默认房间名
                    null,                  // 无密码
                    userId                 // 第一个加入的人当房主
            );

            // 根据房间号查房间信息，拿到真正的 roomId（UUID）
            RoomPO room = gameRoomService.findByRoomNo(roomNo);
            if (room == null) {
                throw new BusinessException("房间创建失败，请重试");
            }

            String actualRoomId = room.getRoomId();

            // 把真正的 roomId 存回 Redis
            redisTemplate.opsForHash().put(key, "roomId", actualRoomId);
            // 有人加入就续期5分钟
            redisTemplate.expire(key, EXPIRE_MINUTES, TimeUnit.MINUTES);

            return actualRoomId;
        }

        // ========== 不是第一个人：直接加入已有房间 ==========
        String roomId = roomIdObj.toString();

        // 根据 roomId（UUID）查房间信息
        RoomPO room = gameRoomService.getByRoomId(roomId);
        if (room == null) {
            // 房间不存在了（可能被解散了），清理掉面对面号码
            redisTemplate.delete(key);
            throw new BusinessException("房间已解散");
        }

        // 加入房间（joinRoom 需要传房间号 roomNo，不是 roomId）
        joinRoomNoPassword(room, userId);//面对面建房没有密码，直接加

        // 续期
        redisTemplate.expire(key, EXPIRE_MINUTES, TimeUnit.MINUTES);

        return roomId;
    }
    /**
     * 检查号码是否存在
     */
    @Override
    public boolean checkCodeExists(String code) {
        String key = F2F_KEY_PREFIX + code;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;


    }
    /**
     * 取消面对面房间
     */
    @Override
    public void cancelFaceToFace(String code, Long userId) {
        String key = F2F_KEY_PREFIX + code;
        Object hostId = redisTemplate.opsForHash().get(key, "hostId");

        if (hostId != null && hostId.toString().equals(userId.toString())) {
            redisTemplate.delete(key);
        }
    }
    /**
     * 获取当前人数
     */
    @Override
    public Integer getPlayerCount(String code) {
        String key = F2F_KEY_PREFIX + code;
        Object roomIdObj = redisTemplate.opsForHash().get(key, "roomId");

        // 无有效正式房间，返回0人
        if (roomIdObj == null || "null".equals(roomIdObj.toString())) {
            return 0;
        }

        // 修复1：roomId是UUID字符串，不再转Long
        String roomId = roomIdObj.toString();
        RoomPO room = gameRoomService.getByRoomId(roomId);

        // 修复2：房间已解散/不存在，清理缓存返回0
        if (room == null) {
            redisTemplate.delete(key);
            return 0;
        }

        return room.getCurrentPlayer();
    }
    /**
     * 内部无密码加入房间，仅内部调用（面对面建群使用）
     * @param room 已查询完成的房间实体
     * @param userId 用户ID
     * @return roomId
     */
    private String joinRoomNoPassword(RoomPO room, Long userId) throws BusinessException {
        // 只保留状态、人数、加锁逻辑，去掉密码校验
        // 校验状态
        if (RoomStatusEnum.PLAYING.getCode().equals(room.getRoomStatus())) {
            throw new BusinessException("游戏已开始，无法加入");
        }
        if (RoomStatusEnum.ENDED.getCode().equals(room.getRoomStatus())) {
            throw new BusinessException("房间已结束");
        }

        // 分布式锁防超员
        String lockKey = "lock:room:join:" + room.getRoomId();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (locked == null || !locked) {
            throw new BusinessException("加入人数过多，请稍后重试");
        }

        try {
            // 重新查最新房间数据
            RoomPO freshRoom = roomMapper.findById(room.getRoomId()).orElse(null);
            if (freshRoom == null) {
                throw new BusinessException("房间不存在");
            }
            // 校验人数
            if (freshRoom.getCurrentPlayer() >= freshRoom.getMaxPlayer()) {
                throw new BusinessException("房间已满");
            }
            // 校验是否已在房间
            boolean exists = roomPlayerMapper.existsByRoomIdAndUserIdAndLeaveTimeIsNull(
                    freshRoom.getRoomId(), userId
            );
            if (exists) {
                return freshRoom.getRoomId();
            }
            // 新增玩家记录、更新人数、缓存
            RoomPlayerPO player = new RoomPlayerPO();
            player.setRoomId(freshRoom.getRoomId());
            player.setUserId(userId);
            player.setIsReady((byte) 0);
            player.setIsHost((byte) 0);
            player.setJoinTime(LocalDateTime.now());
            roomPlayerMapper.save(player);

            freshRoom.setCurrentPlayer(freshRoom.getCurrentPlayer() + 1);
            roomMapper.save(freshRoom);
            cacheRoomInfo(freshRoom);
            return freshRoom.getRoomId();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    private void cacheRoomInfo(RoomPO room) {
        String key = "room:info:" + room.getRoomId();
        redisTemplate.opsForValue().set(key, room, 30, TimeUnit.MINUTES);
    }
}
