/**
 * @Author: 杜江
 * @Description: dm投票服务接口
 * @DateTime: 2026/6/25 12:09
 * @Component:
 **/
package com.wn.service.dm;

import com.wn.entity.dm.RoomVotePO;

import java.util.List;
import java.util.Map;

public interface DmVoteService {

    void startVoting(String roomId);
    void endVoting(String roomId);
    boolean isVoting(String roomId);

    void submitVote(String roomId, Long playerId, Long voteRoleId);

    List<RoomVotePO> getVoteResults(String roomId);
    Map<Long, Long> getVoteCount(String roomId);
}
