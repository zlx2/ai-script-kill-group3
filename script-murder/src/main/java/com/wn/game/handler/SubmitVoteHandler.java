package com.wn.game.handler;

import com.wn.entity.dm.RoomVotePO;
import com.wn.entity.game.GameEventPO;
import com.wn.entity.room.RoomPO;
import com.wn.entity.room.RoomPlayerPO;
import com.wn.game.*;
import com.wn.mapper.dm.DmRoomVoteMapper;
import com.wn.mapper.room.RoomMapper;
import com.wn.mapper.room.RoomPlayerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 提交投票处理器
 */
@Component
@RequiredArgsConstructor
public class SubmitVoteHandler implements RoomCommandHandler {

    private final DmRoomVoteMapper dmRoomVoteMapper;
    private final RoomPlayerMapper roomPlayerMapper;
    private final RoomMapper roomMapper;

    @Override
    public CommandType type() {
        return CommandType.SUBMIT_VOTE;
    }

    @Override
    public Set<GamePhase> allowedPhases() {
        return Set.of(GamePhase.VOTING);
    }

    @Override
    public CommandResult handle(RoomPO room, RoomCommand command) {
        Long targetRoleId = command.payload() != null
                ? Long.valueOf(command.payload().get("targetRoleId").toString())
                : null;
        if (targetRoleId == null) {
            return CommandResult.fail("缺少投票目标");
        }

        // 检查玩家是否在房间
        RoomPlayerPO player = roomPlayerMapper
                .findByRoomIdAndUserIdAndLeaveTimeIsNull(room.getRoomId(), command.userId());
        if (player == null) {
            return CommandResult.fail("玩家不在房间内");
        }

        // 检查是否已投过
        if (dmRoomVoteMapper.existsByRoomIdAndPlayerId(room.getRoomId(), command.userId())) {
            return CommandResult.fail("已投票，不能重复投票");
        }

        // 保存投票
        RoomVotePO vote = new RoomVotePO();
        vote.setRoomId(room.getRoomId());
        vote.setPlayerId(command.userId());
        vote.setVoteRoleId(targetRoleId);
        dmRoomVoteMapper.save(vote);

        List<GameEventPO> events = new ArrayList<>();

        // 投票事件
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", command.userId());
        payload.put("targetRoleId", targetRoleId);

        GameEventPO voteEvent = new GameEventPO();
        voteEvent.setRoomId(room.getRoomId());
        voteEvent.setUserId(command.userId());
        voteEvent.setEventType("VOTE_SUBMITTED");
        voteEvent.setVisibility("PRIVATE");
        voteEvent.setTargetUserId(command.userId());
        voteEvent.setPayloadJson(new com.alibaba.fastjson2.JSONObject(payload).toString());
        events.add(voteEvent);

        // 检查是否所有人都投票了
        long playerCount = roomPlayerMapper.findByRoomIdAndLeaveTimeIsNull(room.getRoomId()).size();
        List<RoomVotePO> allVotes = dmRoomVoteMapper.findByRoomId(room.getRoomId());

        if (allVotes.size() >= playerCount) {
            // 所有人都投完了 → 投票结束事件
            Map<String, Object> finishPayload = new HashMap<>();
            finishPayload.put("totalVotes", allVotes.size());

            GameEventPO finishEvent = new GameEventPO();
            finishEvent.setRoomId(room.getRoomId());
            finishEvent.setUserId(null);
            finishEvent.setEventType("VOTE_FINISHED");
            finishEvent.setVisibility("PUBLIC");
            finishEvent.setPayloadJson(new com.alibaba.fastjson2.JSONObject(finishPayload).toString());
            events.add(finishEvent);
        }

        return CommandResult.ok("投票成功", events);
    }
}
