package com.wn.service.oss;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {
    String updateAvatar(MultipartFile file);
}
