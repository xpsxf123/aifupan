package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.VideoMarkLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.VideoMarkBo;
import com.jiuyu.replay.words.bo.VideoMarkListBo;
import com.jiuyu.replay.words.vo.VideoMarkInfoVo;
import com.jiuyu.replay.words.vo.VideoMarkListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 视频标记
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-10 18:09:05
 */
@RestController
@CrossOrigin
@RequestMapping("replay/videomark")
@Tag(name = "视频标记")
public class VideoMarkController {

    @Resource
    private VideoMarkLogic videoMarkLogic;

    /**
     * 视频标记列表
     * @param videoMarkListBo 视频标记列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "视频标记列表")
    public R<PageUtils<VideoMarkListVo>> list(@Parameter(description = "视频标记列表查询参数", required = true) @RequestBody VideoMarkListBo videoMarkListBo){

        return videoMarkLogic.queryPage(videoMarkListBo);
    }

    /**
     * 根据文件uuid唯一标识获取标记列表
     * @param uuid 视频标记唯一标识uuid
     * @return
     */
    @GetMapping("/listByUUID")
    @Operation(summary = "根据文件uuid唯一标识获取标记列表")
    public R<List<VideoMarkListVo>> listByUUID(@Parameter(description = "视频标记id", required = true) @RequestParam("uuid") String uuid){

        return videoMarkLogic.listByUUID(uuid);
    }


    /**
     * 视频标记信息
     * @param id 视频标记id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "视频标记信息")
    public R<VideoMarkInfoVo> info(@Parameter(description = "视频标记id", required = true) @RequestParam("id") Long id){

        return videoMarkLogic.info(id);
    }

    /**
     * 新增视频标记
     * @param videoMarkBo 视频标记对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增视频标记")
    public R<String> save(@Parameter(description = "视频标记对象", required = true) @RequestBody VideoMarkBo videoMarkBo){

        return videoMarkLogic.save(videoMarkBo);
    }

    /**
     * 修改视频标记
     * @param videoMarkBo 视频标记对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改视频标记")
    public R<String> update(@Parameter(description = "视频标记对象", required = true) @RequestBody VideoMarkBo videoMarkBo){

        return videoMarkLogic.update(videoMarkBo);
    }

    /**
     * 删除视频标记
     * @param id 视频标记id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除视频标记")
    public R<String> delete(@Parameter(description = "视频标记id", required = true) @RequestParam("id") Long id){

        return videoMarkLogic.delete(id);
    }

}
