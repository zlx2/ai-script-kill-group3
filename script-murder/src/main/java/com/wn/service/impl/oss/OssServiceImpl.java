package com.wn.service.impl.oss;

import com.aliyun.oss.OSS;
import com.wn.entity.script.ScriptPO;
import com.wn.entity.user.Userinfo;
import com.wn.mapper.auth.UserinfoMapper;
import com.wn.mapper.script.ScriptMapper;
import com.wn.service.oss.OssService;
import com.wn.utils.content.UserContent;
import com.wn.utils.exception.GlobalException;
import com.wn.utils.oss.OssUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * 阿里云OSS服务实现类
 * 上传图片到OSS并更新数据库中的图片地址
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    private final OssUploadUtil ossUploadUtil;
    private final UserinfoMapper userinfoMapper;
    private final ScriptMapper scriptMapper;


    // ==================== 主入口 ======================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadImage(MultipartFile file, String type, Long bizId) {
        validateFile(file);

        String oldImageUrl;
        String newImageUrl;

        if ("avatar".equalsIgnoreCase(type)) {
            oldImageUrl = uploadAvatar(file);
        } else if ("coverImage".equalsIgnoreCase(type)) {
            oldImageUrl = uploadCover(file, bizId);
        } else {
            throw new GlobalException(400, "不支持的图片类型: " + type + "，请使用 avatar 或 coverImage");
        }

        deleteOldImageAsync(oldImageUrl);
        return getNewImageUrl(type, bizId);
    }


    // ==================== 业务处理方法 =========================================================================

    /**
     * 上传头像
     */
    private String uploadAvatar(MultipartFile file) {
        Long userId = getCurrentUserId();
        Userinfo user = findUserById(userId);

        String oldAvatar = user.getAvatar();
        String newAvatar = ossUploadUtil.upload(file, "avatars");
        user.setAvatar(newAvatar);

        log.info("用户 {} 头像更新成功: {}", userId, newAvatar);
        return oldAvatar;
    }

    /**
     * 上传封面
     */
    private String uploadCover(MultipartFile file, Long scriptId) {
        validateBizId(scriptId);

        // 获取当前登录用户名
        String currentUsername = getCurrentUsername();

        // 查询剧本
        ScriptPO script = findScriptById(scriptId);

        // 权限校验：只有创作者能修改封面
        validateCreator(script.getAuthor(), currentUsername);

        String oldCover = script.getCoverImage();
        String newCover = ossUploadUtil.upload(file, "covers");
        script.setCoverImage(newCover);

        log.info("剧本 {} 封面更新成功，创作者：{}", scriptId, currentUsername);
        return oldCover;
    }


    // ==================== 通用校验方法 ===========================================================================

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GlobalException(400, "文件不能为空");
        }
    }

    private Long getCurrentUserId() {
        Long userId = UserContent.getUserIdHolder();
        if (userId == null) {
            throw new GlobalException(401, "用户未登录，请重新登录");
        }
        return userId;
    }

    private String getCurrentUsername() {
        String username = UserContent.getUsernameHolder();
        if (username == null || username.isEmpty()) {
            throw new GlobalException(401, "用户未登录，请重新登录");
        }
        return username;
    }

    private void validateBizId(Long bizId) {
        if (bizId == null) {
            throw new GlobalException(400, "上传封面时必须指定剧本ID");
        }
    }

    /**
     * 校验操作者是否为剧本创作者
     */
    private void validateCreator(String creatorName, String currentUsername) {
        if (creatorName == null || creatorName.isEmpty()) {
            throw new GlobalException(400, "剧本缺少创作者信息，请联系管理员");
        }
        if (!creatorName.equals(currentUsername)) {
            throw new GlobalException(403, "您没有权限修改此剧本的封面，只有创作者 " + creatorName + " 可以修改");
        }
    }


    // ==================== 数据库查询方法 =======================================================================

    private Userinfo findUserById(Long userId) {
        return userinfoMapper.findById(userId)
                .orElseThrow(() -> new GlobalException(404, "用户不存在"));
    }

    private ScriptPO findScriptById(Long scriptId) {
        return scriptMapper.findById(scriptId)
                .orElseThrow(() -> new GlobalException(404, "剧本不存在"));
    }

    private String getNewImageUrl(String type, Long bizId) {
        if ("avatar".equalsIgnoreCase(type)) {
            Long userId = getCurrentUserId();
            return findUserById(userId).getAvatar();
        } else if ("coverImage".equalsIgnoreCase(type)) {
            return findScriptById(bizId).getCoverImage();
        }
        return null;
    }


    // ==================== 异步删除方法 ======================================================================

    private void deleteOldImageAsync(String oldImageUrl) {
        if (oldImageUrl == null || oldImageUrl.isEmpty()) {
            return;
        }
        if (isDefaultImage(oldImageUrl)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String objectKey = extractObjectKeyFromUrl(oldImageUrl);
                if (objectKey != null) {
                    ossUploadUtil.delete(objectKey);
                    log.info("旧图片删除成功: {}", objectKey);
                }
            } catch (Exception e) {
                log.warn("旧图片删除失败，不影响主流程: {}", e.getMessage());
            }
        });
    }

    private boolean isDefaultImage(String imageUrl) {
        return imageUrl != null && (
                imageUrl.contains("default-avatar") ||
                        imageUrl.contains("default-coverImage")
        );
    }

    private String extractObjectKeyFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                return path.substring(1);
            }
            return path;
        } catch (Exception e) {
            log.warn("提取 objectKey 失败: {}", e.getMessage());
            return null;
        }
    }
}

