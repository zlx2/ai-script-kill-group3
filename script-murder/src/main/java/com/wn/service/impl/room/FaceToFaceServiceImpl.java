/**
 * @Author: 鱼
 * @Description:面对面建房服务
 * @DateTime: 2026/6/23 17:41
 * @Component:
 **/
package com.wn.service.impl.room;

import com.wn.service.exception.BusinessException;
import com.wn.service.room.FaceToFaceService;
import com.wn.service.room.GameRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FaceToFaceServiceImpl implements FaceToFaceService {
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
     */
    @Override
    public Long joinFaceToFace(String code, Long userId) throws BusinessException {
        return null;
    }
    /**
     * 检查号码是否存在
     */
    @Override
    public boolean checkCodeExists(String code) {
        return false;
    }

    @Override
    public void cancelFaceToFace(String code, Long userId) {

    }

    @Override
    public Integer getPlayerCount(String code) {
        return 0;
    }
}
