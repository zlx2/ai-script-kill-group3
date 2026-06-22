package com.wn.service.auth;




import com.wn.controller.auth.dto.UserinfoDTO;
import com.wn.controller.auth.vo.UserinfoVO;

import java.util.Map;

public interface UserService {

    void sendEmail(Map<String, String> params);

    String register(UserinfoDTO userInfo);

    UserinfoVO login(UserinfoDTO userinfoDTO);
}
