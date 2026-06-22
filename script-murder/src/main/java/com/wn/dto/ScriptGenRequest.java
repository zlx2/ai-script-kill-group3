package com.wn.dto;

import lombok.Data;

/**
 * @Author: 杜江
 * @Description:
 * @DateTime: 2026/6/22 18:55
 * @Component:
 **/
@Data
public class ScriptGenRequest {
    private String theme;
    private String type;
    private Integer playerCount;
    private Integer difficulty;
    private String backgroundDesc;
}
