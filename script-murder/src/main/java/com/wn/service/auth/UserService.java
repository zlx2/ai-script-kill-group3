package com.wn.service.auth;




import com.wn.controller.auth.dto.UserinfoDTO;
import com.wn.controller.auth.vo.UserinfoVO;
import com.wn.entity.user.Userinfo;

import java.util.List;
import java.util.Map;

public interface UserService {

    void sendEmail(Map<String, String> params);

    String register(UserinfoDTO userInfo);

    UserinfoVO login(UserinfoDTO userinfoDTO);

    Userinfo getById(Long hostId);

    List<Userinfo> listByIds(List<Long> userIds);
}
