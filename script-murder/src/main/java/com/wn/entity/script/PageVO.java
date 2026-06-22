/**
 * @Author: 弗
 * @Description:
 * @DateTime: 2026/6/22 16:58
 * @Component:
 **/
package com.wn.entity.script;
import lombok.Data;
import org.springframework.data.domain.Page;
import java.util.List;

@Data
public class PageVO<T> {
    private List<T> records;
    private Long total;
    private Integer pages;
    private Integer current;
    private Integer size;

    public static <T> PageVO<T> convert(Page<T> page) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(page.getContent());
        vo.setTotal(page.getTotalElements());
        vo.setPages(page.getTotalPages());
        vo.setCurrent(page.getNumber() + 1);
        vo.setSize(page.getSize());
        return vo;
    }
}
