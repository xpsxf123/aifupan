package com.jiuyu.governance.common.controller;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.common.pojo.response.PresignedUploadResponse;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import com.jiuyu.governance.plugins.oss.storage.PresignedUploadResult;
import com.jiuyu.governance.plugins.oss.storage.impl.ImagesStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 公共-OSS上传接口
 *
 * @author lj
 */
@GovernanceUser
@RestController
@RequestMapping("/api/governance/oss")
@RequiredArgsConstructor
public class OssController {

    private static final Duration UPLOAD_URL_EXPIRATION = Duration.ofMinutes(30);
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final ImagesStorageService imagesStorageService;

    /**
     * 获取图片预上传链接
     *
     * @param suffix 文件后缀（如 jpg、.png）
     * @return 预上传链接信息
     */
    @GetMapping("/image/presigned-upload")
    public ApiResponse<PresignedUploadResponse> getImagePresignedUploadUrl(@RequestParam String suffix) {
        // 处理后缀：确保以 . 开头
        suffix = suffix.trim();
        if (!suffix.startsWith(".")) {
            suffix = "." + suffix;
        }

        // 构建路径：img/年/月/日/uuid.后缀
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ossKey = "img/" + datePath + "/" + uuid + suffix;

        // 生成预上传链接
        PresignedUploadResult result = imagesStorageService.generatePresignedUploadUrl(ossKey, UPLOAD_URL_EXPIRATION);
        LocalDateTime expireTime = LocalDateTime.now().plus(UPLOAD_URL_EXPIRATION);

        return ApiResponse.success(new PresignedUploadResponse(result.getUploadUrl(), result.getOssKey(), expireTime));
    }
}
