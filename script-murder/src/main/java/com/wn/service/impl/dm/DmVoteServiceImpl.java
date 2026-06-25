/**
 * @Author: 杜江
 * @Description: dm投票服务实现类
 * @DateTime: 2026/6/25 12:10
 * @Component:
 **/
package com.wn.service.impl.dm;

import com.wn.entity.dm.DmRoomStatePO;
import com.wn.entity.dm.RoomVotePO;
import com.wn.mapper.dm.DmRoomStateMapper;
import com.wn.mapper.dm.DmRoomVoteMapper;
import com.wn.service.dm.DmVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DmVoteServiceImpl implements DmVoteService {

    private final DmRoomVoteMapper roomVoteMapper;
    private final DmRoomStateMapper roomStateMapper;

    @Override
    @Transactional
    public void startVoting(String roomId) {
        DmRoomStatePO state = roomStateMapper.findByRoomId(roomId).orElse(null);
        if (state != null) {
            state.setIsVoting((byte) 1);
            roomStateMapper.save(state);
        }
    }

    @Override
    @Transactional
    public void endVoting(String roomId) {
        DmRoomStatePO state = roomStateMapper.findByRoomId(roomId).orElse(null);
        if (state != null) {
            state.setIsVoting((byte) 0);
            roomStateMapper.save(state);
        }
    }

    @Override
    public boolean isVoting(String roomId) {
        DmRoomStatePO state = roomStateMapper.findByRoomId(roomId).orElse(null);
        return state != null && state.getIsVoting() == 1;
    }

    @Override
    @Transactional
    public void submitVote(String roomId, Long playerId, Long voteRoleId) {
        if (roomVoteMapper.existsByRoomIdAndPlayerId(roomId, playerId)) {
            return;
        }

        RoomVotePO vote = new RoomVotePO();
        vote.setRoomId(roomId);
        vote.setPlayerId(playerId);
        vote.setVoteRoleId(voteRoleId);
        roomVoteMapper.save(vote);
    }

    @Override
    public List<RoomVotePO> getVoteResults(String roomId) {
        return roomVoteMapper.findByRoomId(roomId);
    }

    @Override
    public Map<Long, Long> getVoteCount(String roomId) {
        List<RoomVotePO> votes = roomVoteMapper.findByRoomId(roomId);
        Map<Long, Long> countMap = new HashMap<>();

        for (RoomVotePO vote : votes) {
            countMap.merge(vote.getVoteRoleId(), 1L, Long::sum);
        }

        return countMap;
    }
}
