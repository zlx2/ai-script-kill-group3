/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/22 15:02
 * @Component:
 **/
package com.wn.controller.script;

import com.wn.entity.R;
import com.wn.entity.script.PageVO;
import com.wn.entity.script.ScriptDto;
import com.wn.entity.script.ScriptPO;
import com.wn.entity.script.ScriptSearchDTO;
import com.wn.service.script.ScriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/script")
@RequiredArgsConstructor
@Slf4j
public class ScriptController {
    @Autowired
    private ScriptService scriptService;
    @GetMapping("/list")
    public R list(ScriptSearchDTO scriptSearchDTO){
        Page<ScriptPO> scriptList = scriptService.searchScript(scriptSearchDTO);
        PageVO<ScriptPO> pageVO = PageVO.convert(scriptList);
        return new R(pageVO);
    }
    @GetMapping("/{id}")
    public R get(@PathVariable Long id){
        ScriptPO scriptPO = scriptService.getById(id);
        return new R(scriptPO);
    }
    @PostMapping("/add")
    public R add(@RequestBody ScriptDto scriptDto){
        scriptService.addScript(scriptDto);
        return R.SUCCESS;
    }
    @PutMapping("/update")
    public R update(@RequestBody ScriptDto scriptDto){
        scriptService.updataScript(scriptDto);
        return R.SUCCESS;
    }
    @DeleteMapping("/{id}")
    public R delete(@PathVariable Long id){
        scriptService.deleteScript(id);
        return R.SUCCESS;
    }
}
