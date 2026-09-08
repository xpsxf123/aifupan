package com.jiuyu.governance.plugins.oss.storage.impl;

import com.aliyun.oss.internal.OSSUtils;
import com.jiuyu.governance.plugins.oss.core.OssTemplate;
import com.jiuyu.governance.plugins.oss.enums.OssBucket;
import com.jiuyu.governance.plugins.oss.storage.AbstractStorageService;
import com.jiuyu.governance.plugins.oss.utils.OssUtils;
import org.springframework.stereotype.Service;

/**
 * AI数据存储服务
 * <p>
 * 对应桶：replay-ai-data
 * </p>
 *
 * @author lj
 */
@Service
public class AiDataStorageService extends AbstractStorageService {

    public AiDataStorageService(OssTemplate ossTemplate) {
        super(ossTemplate);
    }

    @Override
    public OssBucket getBucket() {
        return OssBucket.AI_DATA;
    }

    /**
     * 获取zip文件中的字符串内容
     *
     * @param path zip文件路径
     * @return zip文件中的字符串内容
     */
    public String getStringToZipPath(String path) {
        byte[] bytes = this.downloadAsBytes(path);
        if (bytes == null) return null;
        return OssUtils.extractTextFromZipBytes(bytes);
    }

}
