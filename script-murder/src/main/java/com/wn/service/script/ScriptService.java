package com.wn.service.script;

import com.wn.entity.script.ScriptDto;
import com.wn.entity.script.ScriptPO;
import com.wn.entity.script.ScriptSearchDTO;
import org.springframework.data.domain.Page;

public interface ScriptService {
    void addScript(ScriptDto scriptDto);

    Page<ScriptPO> searchScript(ScriptSearchDTO scriptSearchDTO);

    void updataScript(ScriptDto scriptDto);

    void deleteScript(Long id);

    ScriptPO getById(Long id);
}
