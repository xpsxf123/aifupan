package com.jiuyu.replay.api.controller.common;

import com.jiuyu.replay.api.logic.third.TencentCosLogic;
import com.jiuyu.replay.common.vo.BatchUploadUrlVo;
import com.jiuyu.replay.common.vo.CosFileInfoVo;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.common.vo.TencentCosTokenVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/20 下午3:21
 */
@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("replay/tencentCos")
@Tag(name = "cos控制器")
public class TencentCosController {

    @Resource
    private TencentCosLogic tencentCosLogic;

    @Operation(summary = "获取cos的私有写，公有读cos临时token")
    @GetMapping("/cosPublicReadTempToken")
    public R<TencentCosTokenVo> cosPublicReadTempToken() throws TencentCloudSDKException {
        return tencentCosLogic.cosPublicReadTempToken();
    }

    @Operation(summary = "获取升级包上传的预签名链接")
    @GetMapping("/getUpgradePackageUploadSignUrl")
    public R<SignUploadUrlVo> getUpgradePackageUploadSignUrl() {
        return tencentCosLogic.getUpgradePackageUploadSignUrl();
    }

    @Operation(summary = "获取cos上传的预签名链接")
    @GetMapping("/getUploadSignUrl")
    public R<SignUploadUrlVo> getUploadSignUrl(String cosKey) {
        return tencentCosLogic.getUploadSignUrl(cosKey);
    }

    @Operation(summary = "批量获取cos上传的预签名链接")
    @PostMapping("/getBatchUploadSignUrl")
    public R<List<BatchUploadUrlVo>> getBatchUploadSignUrl(@RequestBody List<String> keys) {
        return tencentCosLogic.getBatchUploadSignUrl(keys);
    }

    @Operation(summary = "COS服务端解压zip包到updatePackageNew目录")
    @PostMapping("/unzipPackage")
    public R<List<CosFileInfoVo>> unzipPackage(@RequestParam String cosKey) {
        return tencentCosLogic.unzipCosPackage(cosKey);
    }

}
