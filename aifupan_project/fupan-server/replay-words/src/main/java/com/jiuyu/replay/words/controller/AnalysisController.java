package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.SensitiveWordsBll;
import com.jiuyu.replay.words.vo.AnalysisResultCloudVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/5 上午10:03
 */
@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("replay/words/analysis")
@Tag(name = "分析内容相关的控制器")
public class AnalysisController {

    @Resource
    private SensitiveWordsBll sensitiveWordsBll;

    /**
     * h5获取在线复盘分析信息
     * @param type 类型 0：视频 1：文件
     * @param uuid 视频或文件的唯一标识 uuid
     * @return
     */
    @Operation(summary = "获取在线复盘分析信息2_0")
    @GetMapping("/mGetOnlineAnalysis")
    public R<AnalysisResultCloudVo> mGetOnlineAnalysis(
            @Parameter(description = "类型 0：视频 1：文件", required = true)@RequestParam Integer type,
            @Parameter(description = "视频或文件的唯一标识 uuid", required = true)@RequestParam String uuid) {

        return sensitiveWordsBll.getOnlineAnalysis2_0(type, uuid);
    }

}
