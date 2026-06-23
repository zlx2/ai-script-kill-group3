package com.wn.utils.oss;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class OssUploadUtil {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(OssUploadUtil.class);

    private final OSS ossClient;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.custom-domain:}") // 如果没配置，则使用默认拼接方式
    private String customDomain;

    /**
     * 上传 MultipartFile（看前端）
     * @param file           文件
     * @param directoryPrefix 目录前缀，如 "avatars"、"products"（不需要斜杠）
     * @return 文件访问 URL
     */
    public String upload(MultipartFile file, String directoryPrefix) {
        try {
            String originalFilename = file.getOriginalFilename();
            String ext = getFileExtension(originalFilename);
            // 生成唯一文件名：UUID + 时间戳
            String fileName = UUID.randomUUID().toString().replace("-", "") + "_" + System.currentTimeMillis() + ext;
            // 按日期分目录：例如 avatars/2026/06/23/xxx.jpg
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectKey = directoryPrefix + "/" + datePath + "/" + fileName;

            // 上传
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            //查看问题所在，临时代码
            try {
                ossClient.putObject(bucketName, objectKey, file.getInputStream(), metadata);
            } catch (OSSException oe) {
                System.out.println("Error Code: " + oe.getErrorCode());
                System.out.println("Error Message: " + oe.getErrorMessage());
                System.out.println("Request ID: " + oe.getRequestId());
            }

            // 返回 URL
            return getFileUrl(objectKey);
        } catch (IOException e) {
            logger.error("OSS 上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }
    /**
     * 预留 InputStream（适用于从其他来源获取的流）
     */
    public String upload(InputStream inputStream, String contentType, String directoryPrefix, String originalFilename) {
        try {
            String ext = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectKey = directoryPrefix + "/" + datePath + "/" + fileName;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            //查看问题所在，临时代码
            try {
                ossClient.putObject(bucketName, objectKey, inputStream, metadata);
            } catch (OSSException oe) {
                System.out.println("Error Code: " + oe.getErrorCode());
                System.out.println("Error Message: " + oe.getErrorMessage());
                System.out.println("Request ID: " + oe.getRequestId());
            }
            return getFileUrl(objectKey);
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                logger.warn("关闭流异常", e);
            }
        }
    }

    /**
     * 删除文件（可选，用于更换头像或删除商品图时）
     */
    public void delete(String objectKey) {
        ossClient.deleteObject(bucketName, objectKey);
        logger.info("删除 OSS 文件：{}", objectKey);
    }

    // -------------------- 私有辅助方法 --------------------

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // 默认后缀
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String getFileUrl(String objectKey) {
        if (customDomain != null && !customDomain.isEmpty()) {
            // 使用自定义域名
            return customDomain + "/" + objectKey;
        } else {
            // 使用标准 OSS 域名
            return "https://" + bucketName + "." + endpoint + "/" + objectKey;
        }
    }
}
