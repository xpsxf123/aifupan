package com.jiuyu.replay.common.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "common")
public class CommonProperties {

    /**
     * 图片存储路径
     */
    private String imgFilePath;
    /**
     * 图片访问地址
     */
    private String showFileUrl;

    /**
     * 客户端文件存储路径
     */
    private String clientFilePath;

    /**
     * 客户端文件访问地址(下载路径)
     */
    private String clientFileUrl;

    /**
     * 当前系统地址
     */
    private String currentSystemAddress;

    /**
     * 设置订单超时时长(分钟)
     */
    private Long orderTimeoutMinutes;
}
