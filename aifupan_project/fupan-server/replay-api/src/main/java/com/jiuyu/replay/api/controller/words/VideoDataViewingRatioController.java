package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.VideoDataViewingRatioLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingRatioBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingRatioListBo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingRatioInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingRatioListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * 数据看盘比例
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-17 17:45:33
 */
@RestController
@CrossOrigin
@RequestMapping("replay/videodataviewingratio")
@Tag(name = "数据看盘比例")
public class VideoDataViewingRatioController {

    @Resource
    private VideoDataViewingRatioLogic videoDataViewingRatioLogic;

    /**
     * 数据看盘比例列表
     * @param videoDataViewingRatioListBo 数据看盘比例列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "数据看盘比例列表")
    public R<PageUtils<VideoDataViewingRatioListVo>> list(@Parameter(description = "数据看盘比例列表查询参数", required = true) @RequestBody VideoDataViewingRatioListBo videoDataViewingRatioListBo){

        return videoDataViewingRatioLogic.queryPage(videoDataViewingRatioListBo);
    }


    /**
     * 数据看盘比例信息
     * @param id 数据看盘比例id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "数据看盘比例信息")
    public R<VideoDataViewingRatioInfoVo> info(@Parameter(description = "数据看盘比例id", required = true) @RequestParam("id") Long id){

        return videoDataViewingRatioLogic.info(id);
    }

    /**
     * 新增数据看盘比例
     * @param videoDataViewingRatioBo 数据看盘比例对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增数据看盘比例")
    public R<String> save(@Parameter(description = "数据看盘比例对象", required = true) @RequestBody VideoDataViewingRatioBo videoDataViewingRatioBo){

        return videoDataViewingRatioLogic.save(videoDataViewingRatioBo);
    }

    /**
     * 修改数据看盘比例
     * @param videoDataViewingRatioBo 数据看盘比例对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改数据看盘比例")
    public R<String> update(@Parameter(description = "数据看盘比例对象", required = true) @RequestBody VideoDataViewingRatioBo videoDataViewingRatioBo){

        return videoDataViewingRatioLogic.update(videoDataViewingRatioBo);
    }

    /**
     * 删除数据看盘比例
     * @param id 数据看盘比例id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除数据看盘比例")
    public R<String> delete(@Parameter(description = "数据看盘比例id", required = true) @RequestParam("id") Long id){

        return videoDataViewingRatioLogic.delete(id);
    }

}
