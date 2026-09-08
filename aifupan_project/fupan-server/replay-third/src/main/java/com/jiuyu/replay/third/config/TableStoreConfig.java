package com.jiuyu.replay.third.config;

import com.alicloud.openservices.tablestore.SyncClient;
import com.jiuyu.replay.third.constant.TableStoreProperties;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/13 下午2:50
 */
@Configuration
public class TableStoreConfig {

    @Resource
    private TableStoreProperties tableStoreProperties;

    @Bean
    public SyncClient syncClient() {
        return new SyncClient(tableStoreProperties.getEndpoint(), tableStoreProperties.getAccessKeyId(), tableStoreProperties.getAccessKeySecret(), tableStoreProperties.getInstanceName());
    }
}
