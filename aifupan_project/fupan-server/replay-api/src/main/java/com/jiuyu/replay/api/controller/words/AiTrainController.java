package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.AiTrainLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AiTrainBo;
import com.jiuyu.replay.words.bo.AiTrainListBo;
import com.jiuyu.replay.words.vo.AiTrainInfoVo;
import com.jiuyu.replay.words.vo.AiTrainListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * AI训练
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-20 18:47:39
 */
@RestController
@CrossOrigin
@RequestMapping("replay/aitrain")
@Tag(name = "AI训练")
public class AiTrainController {

    @Resource
    private AiTrainLogic aiTrainLogic;

    /**
     * AI训练列表
     * @param aiTrainListBo AI训练列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "AI训练列表")
    public R<PageUtils<AiTrainListVo>> list(@Parameter(description = "AI训练列表查询参数", required = true) @RequestBody AiTrainListBo aiTrainListBo){

        return aiTrainLogic.queryPage(aiTrainListBo);
    }


    /**
     * AI训练信息
     * @param id AI训练id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "AI训练信息")
    public R<AiTrainInfoVo> info(@Parameter(description = "AI训练id", required = true) @RequestParam("id") Long id){

        return aiTrainLogic.info(id);
    }

    /**
     * 后台完成训练
     * @param id AI训练id
     * @return
     */
    @GetMapping("/completeTrain")
    @Operation(summary = "后台完成训练")
    public R<String> completeTrain(@Parameter(description = "AI训练id", required = true) @RequestParam("id") Long id){

        return aiTrainLogic.completeTrain(id);
    }

    /**
     * 根据视频id获取AI训练信息
     * @param videoId 视频id
     * @return
     */
    @GetMapping("/infoByVideoId")
    @Operation(summary = "根据视频id获取AI训练信息")
    public R<AiTrainInfoVo> infoByVideoId(@Parameter(description = "视频id", required = true) @RequestParam("videoId") String videoId){

        return aiTrainLogic.infoByVideoId(videoId);
    }

    /**
     * 新增AI训练
     * @param aiTrainBo AI训练对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增AI训练")
    public R<String> save(@Parameter(description = "AI训练对象", required = true) @RequestBody AiTrainBo aiTrainBo){

        return aiTrainLogic.save(aiTrainBo);
    }

    /**
     * 修改AI训练
     * @param aiTrainBo AI训练对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改AI训练")
    public R<String> update(@Parameter(description = "AI训练对象", required = true) @RequestBody AiTrainBo aiTrainBo){

        return aiTrainLogic.update(aiTrainBo);
    }

    /**
     * 删除AI训练
     * @param id AI训练id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除AI训练")
    public R<String> delete(@Parameter(description = "AI训练id", required = true) @RequestParam("id") Long id){

        return aiTrainLogic.delete(id);
    }

}
