package com.jiuyu.replay.common.controller;

import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/10 18:36
 */
@Slf4j
@RestController
@RequestMapping("replay/common/test")
@Tag(name = "cs")
@AllArgsConstructor
public class TestCommonController {

    private final AiOssUtils aiOssUtils;

    @GetMapping("/getSignUploadUrl")
    @Operation(summary = "获取测试的预上传地址")
    public R<SignUploadUrlVo> getSignUploadUrl(String key) {
        SignUploadUrlVo signUploadUrl = aiOssUtils.getSignUploadUrl("test/" + key);
        return R.ok(StrUtil.format("curl -X PUT -T {} \"{}\"", signUploadUrl.getOssKey(), signUploadUrl.getSignedUrl()), signUploadUrl);
    }

    @GetMapping("/getSignDownloadUrl")
    @Operation(summary = "获取测试的预下载地址")
    public R<String> getSignDownloadUrl(String key) {
        return R.ok("", aiOssUtils.getSignDownloadUrl("test/" + key));
    }


}
