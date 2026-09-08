package com.jiuyu.replay.api.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置文件参数
 * @author RayChou
 * @date 2025/5/27 13:54
 */
@Data
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {


    private JobAdminProperties admin = new JobAdminProperties();
    private JobExecutorProperties executor = new JobExecutorProperties();

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public class JobAdminProperties {
        private String accessToken;
        private String addresses;
        private Integer timeout;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public class JobExecutorProperties {
        private String appName;
        private String address;
        private String ip;
        private int port;
        private String logPath;
        private int logRetentionDays;
    }
}
