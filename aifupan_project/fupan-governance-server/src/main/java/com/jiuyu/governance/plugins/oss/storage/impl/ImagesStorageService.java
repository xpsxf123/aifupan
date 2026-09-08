package com.jiuyu.governance.plugins.oss.storage.impl;

import com.jiuyu.governance.plugins.oss.core.OssTemplate;
import com.jiuyu.governance.plugins.oss.enums.OssBucket;
import com.jiuyu.governance.plugins.oss.storage.AbstractStorageService;
import org.springframework.stereotype.Service;

/**
 * 图片存储服务
 * <p>
 * 对应桶：replay-images
 * </p>
 *
 * @author lj
 */
@Service
public class ImagesStorageService extends AbstractStorageService {

    public ImagesStorageService(OssTemplate ossTemplate) {
        super(ossTemplate);
    }

    @Override
    public OssBucket getBucket() {
        return OssBucket.IMAGES;
    }
}
