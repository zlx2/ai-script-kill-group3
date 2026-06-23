/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/22 15:07
 * @Component:
 **/
package com.wn.service.impl.script;

import com.wn.entity.script.ScriptDto;
import com.wn.entity.script.ScriptPO;
import com.wn.entity.script.ScriptPOBackups;
import com.wn.entity.script.ScriptSearchDTO;
import com.wn.mapper.script.ScriptBackupsMapper;
import com.wn.mapper.script.ScriptMapper;
import com.wn.service.script.ScriptService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class ScriptServiceImpl implements ScriptService {
    @Autowired
    private ScriptMapper scriptMapper;
    @Autowired
    private ScriptBackupsMapper scriptBackupsMapper;
    @Override
    public void addScript(ScriptDto scriptDto) {
        //dto转po
        ScriptPO scriptPO = dto2PO(scriptDto);
        scriptMapper.save(scriptPO);
    }

    @Override
    public ScriptPO addScriptAndReturn(ScriptDto scriptDto) {
        ScriptPO scriptPO = dto2PO(scriptDto);
        return scriptMapper.save(scriptPO);
    }

    @Override
    public Page<ScriptPO> searchScript(ScriptSearchDTO dto) {
        // 1. 动态拼接查询条件
        Specification<ScriptPO> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 固定条件：已上架、未删除
            predicates.add(cb.equal(root.get("status"), (byte) 1));

            // 条件1：剧本类型
            if (dto.getScriptType() != null && !dto.getScriptType().isBlank()) {
                predicates.add(cb.equal(root.get("scriptType"), dto.getScriptType()));
            }
            // 条件2：难度
            if (dto.getDifficulty() != null) {
                predicates.add(cb.equal(root.get("difficulty"), dto.getDifficulty()));
            }
            // 条件3：剧本来源
            if (dto.getIsAiGenerated() != null) {
                predicates.add(cb.equal(root.get("isAiGenerated"), dto.getIsAiGenerated()));
            }
            // 条件4：玩家人数区间
            String range = dto.getPlayerRange();
            if (range != null && !range.isBlank()) {
                switch (range) {
                    case "2-4":
                        predicates.add(cb.between(root.get("playerCount"), 2, 4));
                        break;
                    case "5-6":
                        predicates.add(cb.between(root.get("playerCount"), 5, 6));
                        break;
                    case "7-8":
                        predicates.add(cb.between(root.get("playerCount"), 7, 8));
                        break;
                    case "9+":
                        predicates.add(cb.greaterThanOrEqualTo(root.get("playerCount"), 9));
                        break;
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 2. 根据sortType构建排序Sort对象
        Sort sort = switch (dto.getSortType()) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createTime");
            case "rating" -> Sort.by(Sort.Direction.DESC, "rating");
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
            case "durationAsc" -> Sort.by(Sort.Direction.ASC, "duration");
            case "durationDesc" -> Sort.by(Sort.Direction.DESC, "duration");
            // 默认：最新上架排序
            default -> Sort.by(Sort.Direction.DESC, "createTime");
        };

        // 3. 分页对象（JPA页码从0开始）
        Pageable pageable = PageRequest.of(dto.getPageNum() - 1, dto.getPageSize(), sort);

        // 4. 分页查询
        return scriptMapper.findAll(spec, pageable);
    }

    @Override
    public void updataScript(ScriptDto scriptDto) {
        // DTO转PO
        ScriptPO scriptPO = dto2PO(scriptDto);
        // 校验ID不能为空（修改必须携带主键）
        if (scriptPO.getScriptId() == null) {
            throw new RuntimeException("修改脚本必须传入id");
        }
        // 根据id全量更新
        scriptMapper.save(scriptPO);
    }

    @Override
    public void deleteScript(Long id) {
        //查询出要删除的剧本信息
        ScriptPO backup = getById(id);
        // 2. DTO/属性拷贝，转成备份实体，清空原有主键
        ScriptPOBackups backupPO = new ScriptPOBackups();
        // 复制业务字段，忽略原id
        BeanUtils.copyProperties(backup, backupPO, "scriptId");
        // 记录原始id、删除时间
        backupPO.setOriginScriptId(backup.getScriptId());
        //存到备份表
        scriptBackupsMapper.save(backupPO);
        // 删除
        scriptMapper.deleteById(id);
    }

    @Override
    public ScriptPO getById(Long id) {
        return scriptMapper.findOneByScriptId(id);
    }

    private ScriptPO dto2PO(ScriptDto scriptDto) {
        ScriptPO scriptPO=new ScriptPO();
        BeanUtils.copyProperties(scriptDto,scriptPO);
        return scriptPO;
    }

}
