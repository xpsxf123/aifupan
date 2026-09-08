package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.VideoDataViewingBll;
import com.jiuyu.replay.words.bo.viewing.UpdateConfuseDataByVideoIdBo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 视频看盘数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@RestController("videoDataViewingControllerNew")
@CrossOrigin
@RequestMapping("replay/words/videoDataViewing")
@Tag(name = "视频看盘数据")
public class VideoDataViewingController {

    @Resource
    private VideoDataViewingBll videoDataViewingBll;

    /**
     * 后台根据视频id重新拉取看盘数据
     * @param videoId 视频id
     * @return
     */
    @GetMapping("/backRePullData")
    @Operation(summary = "后台根据视频id重新拉取看盘数据")
    public R<String> backRePullData(@Parameter(description = "视频id", required = true) @RequestParam("videoId") String videoId){

        return videoDataViewingBll.backRePullData(videoId);
    }

    /**
     * 根据视频id获取本段视频的数据看板数据
     * @param videoId 视频id
     * @return
     */
    @GetMapping("/paragraphInfoByVideoId")
    @Operation(summary = "根据视频id获取本段视频的数据看板数据")
    public R<VideoDataViewingConfuseInfoVo> paragraphInfoByVideoId(@Parameter(description = "视频id", required = true) @RequestParam("videoId") String videoId){

        return videoDataViewingBll.paragraphInfoByVideoId(videoId);
    }

    /**
     * 根据视频id获取数据看盘数据
     * <p>
     * 优先返回本段数据，如果本段数据无效或不存在则返回混淆数据
     * </p>
     *
     * @param videoId 视频id
     * @return 数据看盘数据
     */
    @GetMapping("/dataViewingByVideoId")
    @Operation(summary = "根据视频id获取数据看盘数据（优先本段数据）")
    public R<VideoDataViewingConfuseInfoVo> dataViewingByVideoId(
            @Parameter(description = "视频id", required = true) @RequestParam("videoId") String videoId) {
        return videoDataViewingBll.dataViewingByVideoId(videoId);
    }

    /**
     * 后台根据视频ID批量修改关联的混淆数据
     * <p>
     * 业务流程：
     * 1. 根据videoId查询混淆表获取该视频的混淆数据
     * 2. 从查询结果中获取videoDataViewingId（关联的数据看盘ID）
     * 3. 如果videoDataViewingId有值，查询所有具有相同videoDataViewingId的混淆数据
     * 4. 批量更新所有关联的混淆数据记录（仅更新非空字段）
     * </p>
     *
     * @param bo 更新参数，包含videoId和需要修改的数据字段
     * @return 更新结果
     */
    @PostMapping("/updateByVideoId")
    @Operation(summary = "后台根据视频ID批量修改关联的混淆数据")
    public R<String> updateByVideoId(
            @Parameter(description = "更新参数", required = true)
            @RequestBody @Valid UpdateConfuseDataByVideoIdBo bo) {
        return videoDataViewingBll.updateByVideoId(bo);
    }
}
