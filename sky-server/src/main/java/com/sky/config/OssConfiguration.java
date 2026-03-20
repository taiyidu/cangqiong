package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类
 * 用于创建阿里云AliOSSUtil对象
 */
@Configuration
@Slf4j
public class OssConfiguration {
    @Bean
    @ConditionalOnMissingBean//条件bean
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProProperties){
        log.info("开始创建阿里云文件上传工具类对象:{}",aliOssProProperties);
         return new AliOssUtil(
                aliOssProProperties.getEndpoint(),
                aliOssProProperties.getAccessKeyId(),
                aliOssProProperties.getAccessKeySecret(),
                aliOssProProperties.getBucketName());
    }
}
