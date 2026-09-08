package com.jiuyu.replay.third.config;

import com.alicloud.openservices.tablestore.SyncClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/13 下午2:56
 */
@Component
public class TableStoreClientManager implements DisposableBean {

    private final SyncClient syncClient;

    public TableStoreClientManager(SyncClient syncClient) {
        this.syncClient = syncClient;
    }

    @Override
    public void destroy() throws Exception {
        if (syncClient != null) {
            syncClient.shutdown();
        }
    }
}