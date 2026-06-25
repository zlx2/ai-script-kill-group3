/**
 * @Author: 弗
 * @Description: 
 * @DateTime: 2026/6/25 11:15
 * @Component: 
 **/
package com.wn.mapper.script;


import com.wn.entity.script.stage.Dto.RoleStageDTO;
import com.wn.entity.script.stage.RoleStageContentPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoleStageContentMapper extends JpaRepository<RoleStageContentPO, Long> {
    /**
     * 根据剧本id、角色id 查询该角色全部分幕章节
     * 关联幕表获取幕号、幕名称
     */
    @Query("SELECT RoleStageDTO(s.stageNo, s.stageName, r.mainContent, r.hintContent, r.unlockStage, true) " +
            "FROM RoleStageContentPO r " +
            "LEFT JOIN ScriptStagePO s ON r.stageId = s.stageId " +
            "WHERE r.scriptId = :scriptId AND r.roleId = :roleId " +
            "ORDER BY s.stageNo ASC")
    List<RoleStageDTO> listRoleStage(@Param("scriptId") Long scriptId, @Param("roleId") Long roleId);
}
