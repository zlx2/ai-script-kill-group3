package com.wn.utils.oss;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
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

/**
 * 腾讯云COS上传工具（S3兼容协议）
 */
@Component
@RequiredArgsConstructor
public class OssUploadUtil {

    private static final Logger logger = LoggerFactory.getLogger(OssUploadUtil.class);

    private final AmazonS3 amazonS3;

    @Value("${cos.bucket-name:script-murder}")
    private String bucketName;

    @Value("${cos.region:ap-guangzhou}")
    private String region;

    @Value("${cos.custom-domain:}")
    private String customDomain;

    public String upload(MultipartFile file, String directoryPrefix) {
        try {
            String originalFilename = file.getOriginalFilename();
            String ext = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID().toString().replace("-", "") + "_" + System.currentTimeMillis() + ext;
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectKey = directoryPrefix + "/" + datePath + "/" + fileName;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectResult result = amazonS3.putObject(bucketName, objectKey, file.getInputStream(), metadata);
            logger.info("COS上传成功: {} -> {}", objectKey, result.getETag());

            return getFileUrl(objectKey);
        } catch (IOException e) {
            logger.error("COS 上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    public String upload(InputStream inputStream, String contentType, String directoryPrefix, String originalFilename) {
        try {
            String ext = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectKey = directoryPrefix + "/" + datePath + "/" + fileName;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(inputStream.available());

            PutObjectResult result = amazonS3.putObject(bucketName, objectKey, inputStream, metadata);
            logger.info("COS上传成功: {} -> {}", objectKey, result.getETag());

            return getFileUrl(objectKey);
        } catch (IOException e) {
            logger.error("COS 上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                logger.warn("关闭流异常", e);
            }
        }
    }

    public void delete(String objectKey) {
        amazonS3.deleteObject(bucketName, objectKey);
        logger.info("删除 COS 文件：{}", objectKey);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String getFileUrl(String objectKey) {
        if (customDomain != null && !customDomain.isEmpty()) {
            return customDomain + "/" + objectKey;
        }
        return "https://" + bucketName + ".cos." + region + ".myqcloud.com/" + objectKey;
    }
}
