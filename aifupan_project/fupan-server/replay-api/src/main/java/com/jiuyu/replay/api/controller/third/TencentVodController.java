package com.jiuyu.replay.api.controller.third;

import com.jiuyu.replay.api.logic.third.TencentVodLogin;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/replay/vod")
public class TencentVodController {

    @Resource
    private TencentVodLogin tencentVodLogin;

    /**
     * 获取vod上传签名
     * @return
     */
    @Operation(summary = "获取vod上传签名")
    @GetMapping("/getVodUploadSign")
    public R<String> getVodUploadSign() {

        return tencentVodLogin.getVodUploadSign();
    }
}
