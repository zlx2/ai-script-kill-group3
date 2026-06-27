/**
 * @Author: 弗
 * @Description: 返回角色分幕剧情
 * @DateTime: 2026/6/25 11:12
 * @Component: 
 **/
package com.wn.entity.script.stage.vo;

import lombok.Data;
import java.util.List;

@Data
public class RoleScriptVO {
    // 剧本标题
    private String title;
    // 当前分配角色名
    private String roleName;
    // 角色背景故事（取第一章mainContent）
    private String background;
    // 所有分幕章节
    private List<ChapterVO> chapters;
    // 角色私人隐藏信息
    private List<PrivateInfoVO> privateInfo;

    // 分幕章节子VO
    @Data
    public static class ChapterVO {
        private Integer chapter;
        private String title;
        private String content;
        private String unlockStage;
    }

    // 私人信息子VO
    @Data
    public static class PrivateInfoVO {
        private String label;
        private String content;
        private String unlockStage;
    }
}