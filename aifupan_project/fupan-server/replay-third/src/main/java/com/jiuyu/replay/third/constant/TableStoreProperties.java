package com.jiuyu.replay.third.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/13 下午2:53
 */
@Component
@Data
@ConfigurationProperties(prefix = "third.ali.tablestore")
public class TableStoreProperties {

    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    private String instanceName;

    private String barrageTableName;

    private String anchorUserTableName;
}
