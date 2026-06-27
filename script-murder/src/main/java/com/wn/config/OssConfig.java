package com.wn.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云COS配置（通过S3兼容协议）
 */
@Configuration
@ConditionalOnProperty(name = "cos.secret-id")
public class OssConfig {

    @Value("${cos.secret-id}")
    private String secretId;

    @Value("${cos.secret-key}")
    private String secretKey;

    @Value("${cos.region:ap-guangzhou}")
    private String region;

    @Bean(destroyMethod = "shutdown")
    public AmazonS3 amazonS3() {
        String endpoint = "https://cos." + region + ".myqcloud.com";
        BasicAWSCredentials creds = new BasicAWSCredentials(secretId, secretKey);
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .withPathStyleAccessEnabled(true)
                .build();
    }
}
