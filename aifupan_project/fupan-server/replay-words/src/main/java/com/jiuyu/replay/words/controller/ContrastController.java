package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.bo.words.ShareContrastCloudBo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.SyncContrastBll;
import com.jiuyu.replay.words.bo.SwitchContrastPositionBo;
import com.jiuyu.replay.words.bo.VideoContrastTypeBo;
import com.jiuyu.replay.words.vo.VideoContrastTypeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("replay/synccontrast")
@Tag(name = "客户端对比记录")
public class ContrastController {

    @Resource
    private SyncContrastBll syncContrastBll;

    /**
     * 分析对比记录到云空间-兼容旧版接口
     * @param contrastId 对比id
     * @return 分享地址
     */
    @GetMapping("/shareContrastToCloud")
    @Operation(summary = "分析对比记录到云空间-兼容旧版接口")
    public R<String> shareContrastToCloud(@Parameter(description = "对比id", required = true) @RequestParam String contrastId) {

        ShareContrastCloudBo shareContrastCloudBo = new ShareContrastCloudBo();
        shareContrastCloudBo.setContrastId(contrastId);
        return syncContrastBll.shareContrastToCloud(shareContrastCloudBo);

    }

    /**
     * 分析对比记录到云空间
     * @param shareContrastCloudBo 分享信息
     * @return 分享地址
     */
    @PostMapping("/shareContrastToCloudPost")
    @Operation(summary = "分析对比记录到云空间")
    public R<String> shareContrastToCloud(@Parameter(description = "视频信息", required = true) @RequestBody ShareContrastCloudBo shareContrastCloudBo) {

        return syncContrastBll.shareContrastToCloud(shareContrastCloudBo);
    }

    /**
     * 判断视频对比类型（优化场次/对标场次）
     * @param videoContrastTypeBo 入参，包含两个视频ID
     * @return 优化视频ID和对标视频ID
     */
    @PostMapping("/determineVideoContrastType")
    @Operation(summary = "判断视频对比类型（优化场次/对标场次）")
    public R<VideoContrastTypeVo> determineVideoContrastType(@RequestBody VideoContrastTypeBo videoContrastTypeBo) {
        return syncContrastBll.determineVideoContrastType(videoContrastTypeBo);
    }

    /**
     * 切换对比定位（交换视频1和视频2、主播1和主播2的位置）
     * @return 切换结果
     */
    @PostMapping("/switchContrastPosition")
    @Operation(summary = "切换对比定位")
    public R<String> switchContrastPosition(@RequestBody SwitchContrastPositionBo switchContrastPositionBo) {
        return syncContrastBll.switchContrastPosition(switchContrastPositionBo.getContrastId());
    }

}
