package com.wn.entity.script;

import lombok.Data;

/**
 * @Author: 杜江
 * @Description:剧本生成请求类
 * @DateTime: 2026/6/24 10:47
 * @Component:
 **/
@Data
public class ScriptGenRequest {
    private String theme;
    private String scriptType;
    private Integer playerCount;
    private String description;
}
