/**
 * @Author: 弗
 * @Description:接收前端查询数据
 * @DateTime: 2026/6/22 16:48
 * @Component:
 **/
package com.wn.entity.script;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScriptSearchDTO {
    //类型
    private String scriptType="";
    //难度
    private Byte difficulty;
    //玩家人数区间
    private String PlayerRange="";
    //剧本来源
    private Byte isAiGenerated;

    //排序标识
    private String sortType="";
    // 分页
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
