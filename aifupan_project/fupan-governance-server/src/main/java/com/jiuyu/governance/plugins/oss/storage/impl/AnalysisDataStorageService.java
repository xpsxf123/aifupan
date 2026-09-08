package com.jiuyu.governance.plugins.oss.storage.impl;

import com.jiuyu.governance.plugins.oss.core.OssTemplate;
import com.jiuyu.governance.plugins.oss.enums.OssBucket;
import com.jiuyu.governance.plugins.oss.storage.AbstractStorageService;
import org.springframework.stereotype.Service;

/**
 * 分析数据存储服务
 * <p>
 * 对应桶：replay-analysis-data
 * </p>
 *
 * @author lj
 */
@Service
public class AnalysisDataStorageService extends AbstractStorageService {

    public AnalysisDataStorageService(OssTemplate ossTemplate) {
        super(ossTemplate);
    }

    @Override
    public OssBucket getBucket() {
        return OssBucket.ANALYSIS_DATA;
    }
}
