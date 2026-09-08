package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.VideoDataViewingLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingCallbackBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingListBo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingContrastVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * 视频看盘数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@RestController
@CrossOrigin
@RequestMapping("replay/videodataviewing")
@Tag(name = "视频看盘数据")
public class VideoDataViewingController {

    @Resource
    private VideoDataViewingLogic videoDataViewingLogic;

    /**
     * 第三方数据平台查询数据回调
     * @param videoDataViewingCallbackBo 回调数据
     * @return
     */
    @PostMapping("/callback")
    @Operation(summary = "第三方数据平台查询数据回调")
    public R<String> dataViewingCallback(@RequestBody VideoDataViewingCallbackBo videoDataViewingCallbackBo) {


        return videoDataViewingLogic.handleDataViewingCallback(videoDataViewingCallbackBo);
    }

    /**
     * 生成视频场次的看盘数据
     * @param videoId 视频id
     * @param isAuto 是否自动生成的 0：否 1：是
     * @param anchorOnlineStatus 主播是否已下播 0：否 1：是
     * @return
     */
    @GetMapping("/createDataViewing")
    @Operation(summary = "生成视频场次的看盘数据")
    public R<String> createDataViewing(@Parameter(description = "视频id", required = true) @RequestParam("videoId") String videoId,
                                       @Parameter(description = "是否自动生成的 0：否 1：是") @RequestParam(required = false) Integer isAuto,
                                       @Parameter(description = "主播是否已下播 0：否 1：是") @RequestParam(required = false) Integer anchorOnlineStatus){

        return videoDataViewingLogic.createDataViewing(videoId, isAuto, anchorOnlineStatus);
    }

    /**
     * 根据视频id获取看盘数据
     * @param videoId 视频id
     * @return
     */
    @GetMapping("/infoByVideoId")
    @Operation(summary = "根据视频id获取看盘数据")
    public R<VideoDataViewingConfuseInfoVo> infoByVideoId(@Parameter(description = "视频id", required = true) @RequestParam("videoId") String videoId){

        return videoDataViewingLogic.infoByVideoId(videoId);
    }

    /**
     * 根据对比id获取看盘数据
     * @param contrastId 对比id
     * @return
     */
    @GetMapping("/infoByContrastId")
    @Operation(summary = "根据对比id获取看盘数据")
    public R<VideoDataViewingContrastVo> infoByContrastId(@Parameter(description = "对比id", required = true) @RequestParam("contrastId") String contrastId){

        return videoDataViewingLogic.infoByContrastId(contrastId);
    }

    /**
     * 视频看盘数据列表
     * @param videoDataViewingListBo 视频看盘数据列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "视频看盘数据列表")
    public R<PageUtils<VideoDataViewingListVo>> list(@Parameter(description = "视频看盘数据列表查询参数", required = true) @RequestBody VideoDataViewingListBo videoDataViewingListBo){

        return videoDataViewingLogic.queryPage(videoDataViewingListBo);
    }


    /**
     * 视频看盘数据信息
     * @param id 视频看盘数据id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "视频看盘数据信息")
    public R<VideoDataViewingInfoVo> info(@Parameter(description = "视频看盘数据id", required = true) @RequestParam("id") Long id){

        return videoDataViewingLogic.info(id);
    }

    /**
     * 新增视频看盘数据
     * @param videoDataViewingBo 视频看盘数据对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增视频看盘数据")
    public R<String> save(@Parameter(description = "视频看盘数据对象", required = true) @RequestBody VideoDataViewingBo videoDataViewingBo){

        return videoDataViewingLogic.save(videoDataViewingBo);
    }

    /**
     * 修改视频看盘数据
     * @param videoDataViewingBo 视频看盘数据对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改视频看盘数据")
    public R<String> update(@Parameter(description = "视频看盘数据对象", required = true) @RequestBody VideoDataViewingBo videoDataViewingBo){

        return videoDataViewingLogic.update(videoDataViewingBo);
    }

    /**
     * 删除视频看盘数据
     * @param id 视频看盘数据id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除视频看盘数据")
    public R<String> delete(@Parameter(description = "视频看盘数据id", required = true) @RequestParam("id") Long id){

        return videoDataViewingLogic.delete(id);
    }

}
