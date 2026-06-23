package com.wn.service.oss;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {
    String uploadImage(MultipartFile file, String type, Long bizId);
}
