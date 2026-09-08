package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.feign.words.AnchorVideoDetailFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorVideoDetailVo;
import com.jiuyu.replay.words.bll.AnchorVideoDetailBll;
import com.jiuyu.replay.words.bo.video.AnchorVideoDetailBo;
import com.jiuyu.replay.words.vo.video.ImportantBarrageStatusVo;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/26 下午3:35
 */
@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("replay/words/anchorVideoDetail")
@Tag(name = "视频详情的控制器")
public class AnchorVideoDetailController {

    @Resource
    private AnchorVideoDetailBll anchorVideoDetailBll;

    @Resource
    private AnchorVideoDetailFeign anchorVideoDetailFeign;

    @PostMapping("updateHasDiagnosisReport")
    @Schema(description = "更新视频是否上传诊断报告字段")
    public R<String> updateHasDiagnosisReport(@RequestBody AnchorVideoDetailBo bo) {
        anchorVideoDetailFeign.updateHasDiagnosisReport(bo.getVideoId(), bo.getHasDiagnosisReport(), bo.getDiagnosisOssName(), bo.getHasDataDiagnosisReport(), bo.getDataDiagnosisOssName());
        return R.ok();
    }

    @GetMapping("infoByVideoId")
    @Schema(description = "获取视频详情")
    public R<AnchorVideoDetailVo> infoByVideoId(String videoId) {
        return R.ok(anchorVideoDetailBll.infoByVideoId(videoId));
    }

    @GetMapping("getImportantBarrageStatus")
    @Schema(description = "获取重要弹幕的状态")
    public R<ImportantBarrageStatusVo> getImportantBarrageStatus(@RequestParam @NotNull(message = "视频id不能为空") String videoId) {
        return R.ok(anchorVideoDetailBll.getImportantBarrageStatus(videoId));
    }
}