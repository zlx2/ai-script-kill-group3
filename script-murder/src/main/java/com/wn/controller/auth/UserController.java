package com.wn.controller.auth;


import com.wn.controller.auth.dto.UserinfoDTO;
import com.wn.controller.auth.vo.UserinfoVO;
import com.wn.entity.R;
import com.wn.service.auth.UserService;
import com.wn.utils.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统用户的登录注册等操作接口
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public R register(@RequestBody UserinfoDTO userInfo){
        String msg=userService.register(userInfo);
        if ("注册成功".equals(msg)) {
            return R.success(msg);
        }
        return R.error(400, msg);
    }
    @PostMapping("/login")
    public R login(@RequestBody UserinfoDTO userInfoDTO){
        UserinfoVO userInfoVO = userService.login(userInfoDTO);
        return new R(userInfoVO);
    }
    //发送验证码
    @PostMapping("/code")
    public R sendEmail(@RequestBody Map<String, String> params) throws GlobalException {
        userService.sendEmail(params);
        return new R(200,"执行成功");
    }
}
