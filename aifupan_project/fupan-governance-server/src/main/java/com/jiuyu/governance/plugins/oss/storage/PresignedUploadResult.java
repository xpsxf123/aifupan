package com.jiuyu.governance.plugins.oss.storage;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 预上传链接结果
 *
 * @author lj
 */
@Data
@AllArgsConstructor
public class PresignedUploadResult {

    /**
     * 预上传链接（PUT方式上传）
     */
    private String uploadUrl;

    /**
     * 完整的 OSS 对象键（含环境前缀）
     */
    private String ossKey;
}
