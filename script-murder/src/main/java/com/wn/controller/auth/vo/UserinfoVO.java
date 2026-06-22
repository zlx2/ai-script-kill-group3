package com.wn.controller.auth.vo;

import lombok.Data;

@Data
public class UserinfoVO {
    Long userId;//用户ID
    String username;//用户名
    String nickname;//昵称
    String avatar;//头像
    String refreshToken;//返回给前端的Token
}
