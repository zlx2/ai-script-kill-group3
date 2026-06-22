package com.wn.controller.auth.dto;


import com.wn.entity.user.Userinfo;
import lombok.Data;

/**
 * 用户信息系统传输层实体类
 */
@Data
public class UserinfoDTO extends Userinfo {

    //新增字段： 邮箱验证码
    private String code;
}
