package com.wn.controller.oss;

import com.wn.entity.R;
import com.wn.service.oss.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class AvatarController {

    private final OssService ossService;

    @PostMapping("/avatar")
    public R uploadAvatar(@RequestParam("file") MultipartFile file) {
        return new R(ossService.updateAvatar(file));
    }

}
