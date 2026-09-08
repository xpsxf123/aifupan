package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.VideoDataViewingConfuseLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseListBo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;




/**
 * 视频看盘混淆后的数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@RestController
@CrossOrigin
@RequestMapping("replay/videodataviewingconfuse")
@Tag(name = "视频看盘混淆后的数据")
public class VideoDataViewingConfuseController {

    @Resource
    private VideoDataViewingConfuseLogic videoDataViewingConfuseLogic;

    /**
     * 视频看盘混淆后的数据列表
     * @param videoDataViewingConfuseListBo 视频看盘混淆后的数据列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "视频看盘混淆后的数据列表")
    public R<PageUtils<VideoDataViewingConfuseListVo>> list(@Parameter(description = "视频看盘混淆后的数据列表查询参数", required = true) @RequestBody VideoDataViewingConfuseListBo videoDataViewingConfuseListBo){

        return videoDataViewingConfuseLogic.queryPage(videoDataViewingConfuseListBo);
    }


    /**
     * 视频看盘混淆后的数据信息
     * @param id 视频看盘混淆后的数据id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "视频看盘混淆后的数据信息")
    public R<VideoDataViewingConfuseInfoVo> info(@Parameter(description = "视频看盘混淆后的数据id", required = true) @RequestParam("id") Long id){

        return videoDataViewingConfuseLogic.info(id);
    }

    /**
     * 新增视频看盘混淆后的数据
     * @param videoDataViewingConfuseBo 视频看盘混淆后的数据对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增视频看盘混淆后的数据")
    public R<String> save(@Parameter(description = "视频看盘混淆后的数据对象", required = true) @RequestBody VideoDataViewingConfuseBo videoDataViewingConfuseBo){

        return videoDataViewingConfuseLogic.save(videoDataViewingConfuseBo);
    }

    /**
     * 修改视频看盘混淆后的数据
     * @param videoDataViewingConfuseBo 视频看盘混淆后的数据对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改视频看盘混淆后的数据")
    public R<String> update(@Parameter(description = "视频看盘混淆后的数据对象", required = true) @RequestBody VideoDataViewingConfuseBo videoDataViewingConfuseBo){

        return videoDataViewingConfuseLogic.update(videoDataViewingConfuseBo);
    }

    /**
     * 删除视频看盘混淆后的数据
     * @param id 视频看盘混淆后的数据id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除视频看盘混淆后的数据")
    public R<String> delete(@Parameter(description = "视频看盘混淆后的数据id", required = true) @RequestParam("id") Long id){

        return videoDataViewingConfuseLogic.delete(id);
    }

}
