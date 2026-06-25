/**
 * @Author: 杜江
 * @Description: 线索管理
 * @DateTime: 2026/6/25 12:08
 * @Component:
 **/
package com.wn.service.impl.dm;

import com.wn.entity.dm.RoomCluePO;
import com.wn.entity.room.RoomPlayerPO;
import com.wn.mapper.dm.DmRoomClueMapper;
import com.wn.mapper.room.RoomPlayerMapper;
import com.wn.service.dm.DmClueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DmClueServiceImpl implements DmClueService {

    private final DmRoomClueMapper roomClueMapper;
    private final RoomPlayerMapper roomPlayerMapper;

    @Override
    @Transactional
    public void grantClue(String roomId, Long playerId, Long clueId) {
        if (hasClue(roomId, playerId, clueId)) {
            return;
        }

        RoomCluePO roomClue = new RoomCluePO();
        roomClue.setRoomId(roomId);
        roomClue.setPlayerId(playerId);
        roomClue.setClueId(clueId);
        roomClueMapper.save(roomClue);
    }

    @Override
    @Transactional
    public void grantClueToAll(String roomId, Long clueId) {
        List<RoomPlayerPO> players = roomPlayerMapper
                .findByRoomIdAndLeaveTimeIsNull(roomId);

        for (RoomPlayerPO player : players) {
            grantClue(roomId, player.getUserId(), clueId);
        }
    }

    @Override
    public List<RoomCluePO> getPlayerClues(String roomId, Long playerId) {
        return roomClueMapper.findByRoomIdAndPlayerId(roomId, playerId);
    }

    @Override
    public List<RoomCluePO> getAllRoomClues(String roomId) {
        return roomClueMapper.findByRoomId(roomId);
    }

    @Override
    public boolean hasClue(String roomId, Long playerId, Long clueId) {
        return roomClueMapper.existsByRoomIdAndPlayerIdAndClueId(roomId, playerId, clueId);
    }
}
