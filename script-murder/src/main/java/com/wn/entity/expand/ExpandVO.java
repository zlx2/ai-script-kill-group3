package com.wn.entity.expand;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 返回给前端的实体类，包装
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpandVO {
    private String delta;       //增量文本
    private boolean finished;   //判断是否结束
}
