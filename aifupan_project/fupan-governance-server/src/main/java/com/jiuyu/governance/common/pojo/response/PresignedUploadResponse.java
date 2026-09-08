package com.jiuyu.governance.common.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预上传链接响应
 *
 * @author lj
 */
@Data
@AllArgsConstructor
public class PresignedUploadResponse {

    /**
     * 预上传链接（PUT方式上传）
     */
    private String uploadUrl;

    /**
     * OSS 对象键（用于后续关联业务）
     */
    private String ossKey;

    /**
     * 链接过期时间
     */
    private LocalDateTime expireTime;
}
