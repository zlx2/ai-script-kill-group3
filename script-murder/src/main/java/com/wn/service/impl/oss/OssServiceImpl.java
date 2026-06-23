package com.wn.service.impl.oss;

import com.aliyun.oss.OSS;
import com.wn.entity.user.Userinfo;
import com.wn.mapper.auth.UserinfoMapper;
import com.wn.service.oss.OssService;
import com.wn.utils.content.UserContent;
import com.wn.utils.exception.GlobalException;
import com.wn.utils.oss.OssUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

/**
 * 阿里云OSS服务实现类
 * 上传头像并更新数据库
 */
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {
    private final OssUploadUtil ossUploadUtil;
    private final UserinfoMapper userinfoMapper;

    /**
     * 上传头像并更新数据库
     * @param file 前端传过来的图片文件
     * @return 新的头像完整URL
     */
    @Override
    @Transactional(rollbackFor = Exception.class)//开启事务,如果异常,回滚事务
    public String updateAvatar(MultipartFile file) {
        if (file.isEmpty()) {
            throw new GlobalException(401,"文件不能为空");
        }
        // 1. 从 ThreadLocal 中获取当前登录用户 ID
        Long userId = UserContent.getUserIdHolder();
        // 2. 上传到 OSS，放在 avatars 目录下
        String newAvatarUrl = ossUploadUtil.upload(file, "avatars");
        // 3. （用于后续删除，节省OSS空间），用户不存在时抛出异常
        Userinfo user = userinfoMapper.findById(userId)
                .orElseThrow(() -> new GlobalException(404,"上传头像时失败，用户不存在"));
        // 4. 先查询旧头像地址
        String oldAvatarUrl = user.getAvatar();
        // 5. 更新数据库中的头像地址，直接修改实体字段，JPA 会在事务提交时自动 UPDATE
        user.setAvatar(newAvatarUrl);
        // 6. 异步删除旧头像
        if (oldAvatarUrl != null && !oldAvatarUrl.contains("default-avatar")) {
            String oldObjectKey = extractObjectKeyFromUrl(oldAvatarUrl);
            if (oldObjectKey != null) {
                CompletableFuture.runAsync(() -> {
                    ossUploadUtil.delete(oldObjectKey);
                });
            }
        }
        return newAvatarUrl;
    }

    /**
     * 从完整 URL 中提取 OSS 的 objectKey
     */
    private String extractObjectKeyFromUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                return path.substring(1);
            }
            return path;
        } catch (Exception e) {
            return null;
        }
    }
}
