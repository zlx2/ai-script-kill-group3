/**
 * @Author: 鱼
 * @Description:
 * @DateTime: 2026/6/22 17:21
 * @Component:
 **/
package com.wn.service.impl.room;

import com.wn.controller.enums.room.GameStageEnum;
import com.wn.controller.enums.room.RoomStatusEnum;
import com.wn.controller.room.vo.RoomDetailVO;
import com.wn.controller.room.vo.RoomPlayerVO;
import com.wn.entity.R;
import com.wn.entity.room.RoomPO;
import com.wn.entity.room.RoomPlayerPO;
import com.wn.entity.script.ScriptPO;
import com.wn.mapper.room.RoomMapper;
import com.wn.mapper.room.RoomPlayerMapper;
import com.wn.service.auth.UserService;
import com.wn.service.exception.BusinessException;
import com.wn.service.room.GameRoomService;
import com.wn.service.script.ScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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
        player.setIsReady((byte) 0);
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

    /**
     * 根据房间号查房间
     */
    @Override
    public Object getByRoomNo(String roomNo) {

        return null;
    }

    @Override
    public RoomDetailVO getRoomDetail(String roomId) {
        return null;
    }

    @Override
    public RoomDetailVO getRoomDetailByNo(String roomNo) {
        return null;
    }

    @Override
    public void transferHost(String roomId, Long targetUserId, Long userId) {

    }

    @Override
    public void kickPlayer(String roomId, Long targetUserId, Long userId) {

    }

    @Override
    public void dismissRoom(String roomId, Long userId) {

    }

    @Override
    public void startGame(String roomId, Long userId) {

    }

    @Override
    public List<RoomPlayerVO> getRoomPlayers(String roomId) {
        return List.of();
    }

    @Override
    public void toggleReady(String roomId, Long userId) {

    }

    @Override
    public void leaveRoom(String roomId, Long userId) {

    }

    @Override
    public String joinRoom(String roomNo, String password, Long userId) {
        return "";
    }
}
