package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.VideoAnalysisRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordBo;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordListBo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordInfoVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * 视频的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@RestController
@CrossOrigin
@RequestMapping("words/videoanalysusrecord")
@Tag(name = "视频的分析记录")
public class VideoAnalysisRecordController {

    @Resource
    private VideoAnalysisRecordLogic videoAnalysisRecordLogic;




    /**
     * 视频的分析记录列表
     * @param videoAnalysisRecordListBo 视频的分析记录列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "视频的分析记录列表")
    public R<PageUtils<VideoAnalysisRecordListVo>> list(@Parameter(description = "视频的分析记录列表查询参数", required = true) @RequestBody VideoAnalysisRecordListBo videoAnalysisRecordListBo){

        return videoAnalysisRecordLogic.queryPage(videoAnalysisRecordListBo);
    }


    /**
     * 视频的分析记录信息
     * @param id 视频的分析记录id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "视频的分析记录信息")
    public R<VideoAnalysisRecordInfoVo> info(@Parameter(description = "视频的分析记录id", required = true) @RequestParam("id") Long id){

        return videoAnalysisRecordLogic.info(id);
    }

    /**
     * 新增视频的分析记录
     * @param videoAnalysisRecordBo 视频的分析记录对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增视频的分析记录")
    public R<String> save(@Parameter(description = "视频的分析记录对象", required = true) @RequestBody VideoAnalysisRecordBo videoAnalysisRecordBo){

        return videoAnalysisRecordLogic.save(videoAnalysisRecordBo);
    }

    /**
     * 修改视频的分析记录
     * @param videoAnalysisRecordBo 视频的分析记录对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改视频的分析记录")
    public R<String> update(@Parameter(description = "视频的分析记录对象", required = true) @RequestBody VideoAnalysisRecordBo videoAnalysisRecordBo){

        return videoAnalysisRecordLogic.update(videoAnalysisRecordBo);
    }

    /**
     * 删除视频的分析记录
     * @param id 视频的分析记录id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除视频的分析记录")
    public R<String> delete(@Parameter(description = "视频的分析记录id", required = true) @RequestParam("id") Long id){

        return videoAnalysisRecordLogic.delete(id);
    }

}
