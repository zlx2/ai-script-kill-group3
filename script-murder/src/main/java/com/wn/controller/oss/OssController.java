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
public class OssController {

    private final OssService ossService;

    /**
     * 上传图片到OSS
     * @param type  上传文件类型，是图片、封面还是其他的？具体看枚举类
     * @param file  上传的图片文件
     * @param bizId 业务ID，用户图像直接传null，其他业务场景传业务ID
     * @return 上传后的图片URL
     */
    @PostMapping("/image")
    public R uploadImage( @RequestParam("type") String type,
                          @RequestParam("file") MultipartFile file,
                          @RequestParam(value = "bizId", required = false) Long bizId) {

        return new R(ossService.uploadImage(file, type, bizId));
    }

}
