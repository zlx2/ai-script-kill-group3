/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/23 12:05
 * @Component:
 **/
package com.wn.test;

import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ScriptKillGameService {
    // 角色基础配置：角色名 = [对外身份, 隐藏秘密]
    private final Map<String, String[]> ROLE_MAP = Map.of(
            "小兰", new String[]{"庄园女仆", "我深夜下毒杀死老爷，伪造密室"},
            "老李", new String[]{"合作商人", "死者欠我巨额欠款，我有杀人动机"},
            "老王", new String[]{"私人医生", "我仅提供普通安眠药，不含毒素"}
    );

    // 游戏阶段
    public enum GameStage {
        OPEN, SELF_INTRO, SEARCH, DISCUSS, VOTE, REVIEW
    }

    @Resource
    private HarnessAgent dmAgent;
    @Resource
    private ScriptKillTools scriptKillTools;
    @Resource
    private RoomRedisStore roomRedisStore;

    // ========== DM统一封装：sessionId = roomId，每个房间独立AI会话记忆（仅内存存储） ==========
    private String dmCall(String roomId, String prompt) {
        String baseSecretRule = """
                硬性规则，全程严格遵守：
                1. 只有REVIEW复盘阶段，才能完整公布凶手、作案手法、所有人隐藏秘密；
                2. OPEN/SELF_INTRO/SEARCH/DISCUSS/VOTE阶段，绝对不能暗示、说出凶手身份；
                3. 玩家询问谁是凶手，统一回复：请完成全部投票后，复盘阶段会公布完整真相；
                4. 线索只能由玩家自行搜证场景获取，你不能手动发放任何线索，不解读线索背后的作案真相；
                5. 你是受控DM，禁止自主调用任何工具，所有搜证、投票操作由玩家手动完成。
                """;
        String fullPrompt = baseSecretRule + "\n=====本次DM指令=====\n" + prompt;
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(fullPrompt)
                .build();
        // 关键：以roomId作为独立会话ID，每个房间DM内存记忆隔离
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId("room_" + roomId)
                .build();
        try {
            // 设置5秒超时，避免无限阻塞接口
            String reply = dmAgent.call(List.of(msg), ctx)
                    .block(Duration.ofSeconds(5))
                    .getTextContent();
            // 写入全局公聊存档
            String log = "【DM】" + reply;
            roomRedisStore.appendGlobalChat(roomId, log);
            return reply;
        } catch (Exception e) {
            String errMsg = "DM调用大模型超时/异常，请稍后重试";
            roomRedisStore.appendGlobalChat(roomId, "【系统提示】" + errMsg);
            return errMsg;
        }
    }

    // 1. 创建/恢复房间（支持暂停续玩，仅房间业务数据持久化）
    public String startGame(String roomId) {
        // 判断Redis是否存在存档，直接续玩
        if (roomRedisStore.roomExists(roomId)) {
            String currentStage = roomRedisStore.getStage(roomId);
            return "房间存档已加载，可继续游戏，当前阶段：" + currentStage;
        }
        // 无存档，初始化全新房间Redis存档
        roomRedisStore.setStage(roomId, GameStage.OPEN.name());
        String prompt = """
                你是剧本杀DM，仅简单介绍案情：庄园主张老爷死于书房密室，茶杯检出氰化物毒素。
                现阶段仅开放角色选择，可选角色：小兰、老李、老王，每个角色只能被一位玩家选择。
                全部玩家选完角色后进入自我介绍阶段，所有线索只能通过玩家自行搜证场景获得，不要透露任何凶手相关信息。
                """;
        String dmReply = dmCall(roomId, prompt);
        roomRedisStore.setStage(roomId, GameStage.SELF_INTRO.name());
        return "房间创建成功，当前阶段：角色选择阶段\nDM：" + dmReply;
    }

    // 2. 用户选角色，绑定 userId <-> roleName
    public String selectRole(String roomId, String userId, String roleName) {
        Set<String> validRoles = scriptKillTools.getAllValidRoles();
        if (!validRoles.contains(roleName)) return "角色不存在，可选：" + validRoles;
        Map<String, String> userBindMap = roomRedisStore.getAllUserBind(roomId);
        // 校验1：角色已被占用
        if (roomRedisStore.roleIsSelected(roomId, roleName)) return "该角色已被其他玩家选择，请更换角色";
        // 校验2：当前用户已绑定过角色
        if (userBindMap.containsKey(userId)) return "你已绑定角色，无法重复选择";

        // 绑定存入Redis
        roomRedisStore.bindUserRole(roomId, userId, roleName);
        roomRedisStore.addSelectedRole(roomId, roleName);

        String identity = ROLE_MAP.get(roleName)[0];
        String log = String.format("【选角成功】用户%s 绑定角色【%s】，身份：%s", userId, roleName, identity);
        roomRedisStore.appendGlobalChat(roomId, log);
        return log;
    }

    // 通用权限校验：校验当前用户是否绑定目标角色
    private boolean checkUserRoleBind(String roomId, String userId, String targetRole) {
        String bindRole = roomRedisStore.getUserBindRole(roomId, userId);
        return targetRole.equals(bindRole);
    }

    // 根据用户ID查询自身绑定角色
    public String getUserBindRole(String roomId, String userId) {
        return roomRedisStore.getUserBindRole(roomId, userId);
    }

    // ========== 玩家搜证（唯一获取线索渠道，Redis持久化） ==========
    public String searchEvidence(String roomId, String userId, String roleName, String scene) {
        if (!checkUserRoleBind(roomId, userId, roleName)) return "操作失败：该角色不是你绑定的角色";
        GameStage stage = GameStage.valueOf(roomRedisStore.getStage(roomId));
        if (!GameStage.SEARCH.equals(stage)) return "当前非搜证阶段";

        String clueResult = scriptKillTools.searchClue(roomId, roleName, scene);
        String[] splitArr = clueResult.split("搜到线索：");
        // 安全判断：分割后长度不足2，直接返回原文本，不取下标1
        String clueText;
        if (splitArr.length >= 2) {
            clueText = splitArr[1];
        } else {
            clueText = clueResult;
        }

        if (clueText.contains("【公开】")) {
            roomRedisStore.appendGlobalChat(roomId, "【搜证-" + roleName + "】" + clueResult);
        } else {
            roomRedisStore.appendGlobalChat(roomId, "【搜证-" + roleName + "】获得一条私有线索（仅本人可见）");
            roomRedisStore.appendPrivateChat(roomId, roleName, "【搜证私有线索】" + clueResult);
        }
        return clueResult;
    }

    // 玩家投票
    public String voteSuspect(String roomId, String userId, String voter, String target) {
        if (!checkUserRoleBind(roomId, userId, voter)) return "投票失败：voter不是你绑定的角色";
        GameStage stage = GameStage.valueOf(roomRedisStore.getStage(roomId));
        if (!GameStage.VOTE.equals(stage)) return "当前非投票阶段";

        String voteLog = scriptKillTools.voteSuspect(roomId, voter, target);
        roomRedisStore.appendGlobalChat(roomId, "【投票】" + voteLog);
        return voteLog;
    }

    // 角色自我介绍
    public String selfIntro(String roomId, String userId, String roleName) {
        if (!checkUserRoleBind(roomId, userId, roleName)) return "自我介绍失败：该角色不是你绑定角色";
        GameStage stage = GameStage.valueOf(roomRedisStore.getStage(roomId));
        if (!GameStage.SELF_INTRO.equals(stage)) return "当前非自我介绍阶段";

        String identity = ROLE_MAP.get(roleName)[0];
        String prompt = String.format("玩家【%s】完成自我介绍，简单友好回应，严禁泄露凶手、任何人隐藏秘密，角色身份：%s", roleName, identity);
        String dmReply = dmCall(roomId, prompt);
        roomRedisStore.addIntroDone(roomId, roleName);

        String log = String.format("【%s自我介绍】DM回应：%s", roleName, dmReply);
        roomRedisStore.appendGlobalChat(roomId, log);

        // 全部角色自我介绍完成，自动切搜证阶段
        Set<String> doneIntro = roomRedisStore.getIntroDone(roomId);
        Set<String> allRoomRoles = roomRedisStore.getSelectedRoles(roomId);
        if (doneIntro.size() == allRoomRoles.size()) {
            dmCall(roomId, """
                    所有玩家自我介绍完毕，进入搜证阶段，每人仅有一次搜证机会；
                    可选场景：tea_room / study_room / maid_room / business_room / doctor_room；
                    私有线索仅本人可见，公开线索全房间可见，所有线索只能靠自主搜证获取。
                    """);
            roomRedisStore.setStage(roomId, GameStage.SEARCH.name());
        }
        return log;
    }

    // 全局公聊发言
    public String publicChat(String roomId, String userId, String roleName, String content) {
        if (!checkUserRoleBind(roomId, userId, roleName)) return "发言失败：该角色不是你绑定角色";
        GameStage stage = GameStage.valueOf(roomRedisStore.getStage(roomId));
        if (GameStage.SELF_INTRO.equals(stage)) return "自我介绍阶段禁止公聊发言";

        String log = String.format("【公聊-%s】：%s", roleName, content);
        roomRedisStore.appendGlobalChat(roomId, log);
        return log;
    }

    // 双人私聊
    public String privateChat(String roomId, String userId, String sender, String receiver, String content) {
        if (!checkUserRoleBind(roomId, userId, sender)) return "私聊失败：发送角色不是你绑定角色";
        GameStage stage = GameStage.valueOf(roomRedisStore.getStage(roomId));
        if (GameStage.SELF_INTRO.equals(stage)) return "自我介绍阶段禁止私聊";
        Set<String> allRoles = roomRedisStore.getSelectedRoles(roomId);
        if (!allRoles.contains(sender) || !allRoles.contains(receiver)) return "发送/接收角色未选择";
        if (sender.equals(receiver)) return "不能和自己发起私聊";

        String log = String.format("【私聊-%s→%s】：%s", sender, receiver, content);
        roomRedisStore.appendPrivateChat(roomId, sender, log);
        roomRedisStore.appendPrivateChat(roomId, receiver, log);
        return log;
    }

    // DM切换游戏阶段
    public String nextGameStage(String roomId) {
        GameStage current = GameStage.valueOf(roomRedisStore.getStage(roomId));
        String prompt = switch (current) {
            case OPEN -> """
                    所有玩家角色选择完毕，进入自我介绍阶段，请玩家依次完成自我介绍。
                    """;
            case SEARCH -> """
                    搜证阶段结束，进入公聊推理阶段，玩家分享搜证获得的线索互相推理，不要剧透真凶。
                    """;
            case DISCUSS -> """
                    公聊推理结束，进入投票阶段，玩家投票指认自己怀疑的嫌疑人。
                    """;
            case VOTE -> {
                String maxSuspect = scriptKillTools.getRoomMaxVoteSuspect(roomId);
                yield String.format("投票结束，进入最终复盘阶段，完整公布：最高票嫌疑人%s、真凶身份、密室作案全过程、全部角色隐藏秘密、所有搜证线索完整真相。", maxSuspect);
            }
            default -> "无法切换阶段，当前阶段：" + current;
        };
        String dmReply = dmCall(roomId, prompt);
        GameStage nextStage = switch (current) {
            case OPEN -> GameStage.SELF_INTRO;
            case SEARCH -> GameStage.DISCUSS;
            case DISCUSS -> GameStage.VOTE;
            case VOTE -> GameStage.REVIEW;
            default -> current;
        };
        roomRedisStore.setStage(roomId, nextStage.name());
        return "切换新阶段：" + nextStage + "\nDM：" + dmReply;
    }

    // ========== 查询接口 ==========
    // 全局公聊历史
    public String getGlobalHistory(String roomId) {
        if (!roomRedisStore.roomExists(roomId)) return "房间不存在，请先创建房间";
        return roomRedisStore.getGlobalChat(roomId);
    }

    public String getRolePrivateHistory(String roomId, String userId, String roleName) {
        // 仅允许查看自己绑定角色的私有记录
        String bindRole = roomRedisStore.getUserBindRole(roomId, userId);
        if (!roleName.equals(bindRole)) {
            return "权限不足，仅可查看自身角色私有记录";
        }
        return roomRedisStore.getPrivateChat(roomId, roleName);
    }

    // 查询自身全部私有线索
    public List<String> getRolePrivateClues(String roomId, String userId, String roleName) {
        String bindRole = roomRedisStore.getUserBindRole(roomId, userId);
        if (!roleName.equals(bindRole)) {
            return List.of("无权限查看该角色私有线索");
        }
        return scriptKillTools.getPlayerPrivateClues(roomId, roleName);
    }

    // 获取房间已选角色列表
    public Set<String> getRoomSelectedRoles(String roomId) {
        return roomRedisStore.getSelectedRoles(roomId);
    }

    // 获取当前房间阶段
    public GameStage getCurrentStage(String roomId) {
        if (!roomRedisStore.roomExists(roomId)) return null;
        String stageStr = roomRedisStore.getStage(roomId);
        if(stageStr == null) return null;
        return GameStage.valueOf(stageStr);
    }

    // 销毁房间（彻底结束游戏）
    public void destroyRoom(String roomId) {
        // 1. 清空业务Redis房间存档（房间阶段、角色、聊天、线索）
        roomRedisStore.destroyAllRoomData(roomId);
        // 2. 清空工具内存缓存：搜证记录、投票记录
        scriptKillTools.clearRoomAllData(roomId);
        // 注：AI对话仅保存在内存，服务重启自动清空，无需Redis删除操作
    }
}