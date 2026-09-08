package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.bo.words.video.SaveSliceCorrelationDataBo;
import com.jiuyu.replay.generic.bo.words.video.VideoSliceBo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.VideoSliceBll;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("replay/videoSlice")
@Tag(name = "视频切片")
public class VideoSliceController {

    @Resource
    private VideoSliceBll videoSliceBll;

    /**
     * 保存视频切片
     *
     * @param videoSliceBo 视频切片参数
     *
     * @return
     */
    @PostMapping("/saveVideoSlice")
    @Operation(summary = "保存视频切片")
    public R<String> saveVideoSlice(@RequestBody @Validated(Insert.class) VideoSliceBo videoSliceBo) {

        return this.videoSliceBll.saveVideoSlice(videoSliceBo);
    }

    /**
     * 保存切片相关数据（websocket、数据看板等）
     * @param saveSliceCorrelationDataBo 请求参数
     * @return
     */
    @PostMapping("/saveSliceCorrelationData")
    @Operation(summary = "保存切片相关数据（websocket、数据看板等）")
    public R<String> saveSliceCorrelationData(@RequestBody @Validated SaveSliceCorrelationDataBo saveSliceCorrelationDataBo) {

        return this.videoSliceBll.saveSliceCorrelationData(saveSliceCorrelationDataBo);
    }

    /**
     * 修改视频切片
     *
     * @param videoSliceBo 视频切片参数
     *
     * @return
     */
    @PostMapping("/updateVideoSlice")
    @Operation(summary = "修改视频切片")
    public R<String> updateVideoSlice(@RequestBody @Validated(Update.class) VideoSliceBo videoSliceBo) {

        return this.videoSliceBll.updateVideoSlice(videoSliceBo);
    }

}
