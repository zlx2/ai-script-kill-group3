/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/24 12:01
 * @Component:
 **/
package com.wn.test;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class RoomRedisStore {
    // 存档过期7天，暂停续玩有效期
    private static final long SAVE_DAY = 7;
    private static final String PREFIX = "script:room:";

    private final StringRedisTemplate strRedis;
    private final RedisTemplate<String, Object> objRedis;

    public RoomRedisStore(StringRedisTemplate strRedisTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.strRedis = strRedisTemplate;
        this.objRedis = redisTemplate;
    }

    // 完整Key拼接
    private String key(String roomId, String suffix) {
        return PREFIX + roomId + ":" + suffix;
    }

    // ========== 1. 游戏阶段 GameStage ==========
    public void setStage(String roomId, String stage) {
        strRedis.opsForValue().set(key(roomId, "stage"), stage, SAVE_DAY, TimeUnit.DAYS);
    }
    public String getStage(String roomId) {
        return strRedis.opsForValue().get(key(roomId, "stage"));
    }

    // ========== 2. 用户-角色绑定 Hash userId -> roleName ==========
    public void bindUserRole(String roomId, String userId, String roleName) {
        strRedis.opsForHash().put(key(roomId, "user_role"), userId, roleName);
        strRedis.expire(key(roomId, "user_role"), SAVE_DAY, TimeUnit.DAYS);
    }
    public String getUserBindRole(String roomId, String userId) {
        Object val = strRedis.opsForHash().get(key(roomId, "user_role"), userId);
        return val == null ? null : val.toString();
    }
    public Map<String, String> getAllUserBind(String roomId) {
        Map<Object, Object> map = strRedis.opsForHash().entries(key(roomId, "user_role"));
        return map.entrySet().stream()
                .collect(Collectors.toMap(k -> k.getKey().toString(), v -> v.getValue().toString()));
    }

    // ========== 3. 已选角色集合 Set ==========
    public void addSelectedRole(String roomId, String role) {
        strRedis.opsForSet().add(key(roomId, "selected_roles"), role);
        strRedis.expire(key(roomId, "selected_roles"), SAVE_DAY, TimeUnit.DAYS);
    }
    public Set<String> getSelectedRoles(String roomId) {
        Set<String> set = strRedis.opsForSet().members(key(roomId, "selected_roles"));
        return set == null ? new HashSet<>() : set;
    }
    public boolean roleIsSelected(String roomId, String role) {
        return Boolean.TRUE.equals(strRedis.opsForSet().isMember(key(roomId, "selected_roles"), role));
    }

    // ========== 4. 完成自我介绍集合 Set ==========
    public void addIntroDone(String roomId, String role) {
        strRedis.opsForSet().add(key(roomId, "intro_done"), role);
        strRedis.expire(key(roomId, "intro_done"), SAVE_DAY, TimeUnit.DAYS);
    }
    public Set<String> getIntroDone(String roomId) {
        Set<String> set = strRedis.opsForSet().members(key(roomId, "intro_done"));
        return set == null ? new HashSet<>() : set;
    }

    // ========== 5. 全局公聊文本 String ==========
    public void appendGlobalChat(String roomId, String text) {
        String k = key(roomId, "global_chat");
        strRedis.opsForValue().append(k, text + "\n");
        strRedis.expire(k, SAVE_DAY, TimeUnit.DAYS);
    }
    public String getGlobalChat(String roomId) {
        String val = strRedis.opsForValue().get(key(roomId, "global_chat"));
        return val == null ? "" : val;
    }

    // ========== 6. 角色私有记录 String ==========
    public void appendPrivateChat(String roomId, String role, String text) {
        String k = key(roomId, "private:" + role);
        strRedis.opsForValue().append(k, text + "\n");
        strRedis.expire(k, SAVE_DAY, TimeUnit.DAYS);
    }
    public String getPrivateChat(String roomId, String role) {
        String val = strRedis.opsForValue().get(key(roomId, "private:" + role));
        return val == null ? "" : val;
    }

    // ========== 销毁房间：清空该房间所有业务存档 ==========
    public void destroyAllRoomData(String roomId) {
        Set<String> keys = strRedis.keys(PREFIX + roomId + ":*");
        if (keys != null && !keys.isEmpty()) {
            strRedis.delete(keys);
        }
    }

    // 判断房间存档是否存在
    public boolean roomExists(String roomId) {
        String k = key(roomId, "stage");
        return Boolean.TRUE.equals(strRedis.hasKey(k));
    }
}
