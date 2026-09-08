package com.jiuyu.replay.api.controller.third;


import com.jiuyu.replay.api.logic.third.AiModelLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.third.bo.AiModelListBo;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiModelListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * AI模型配置表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
@RestController
@CrossOrigin
@RequestMapping("replay/aimodel")
@Tag(name = "AI模型配置表")
public class AiModelController {

    @Resource
    private AiModelLogic aiModelLogic;

    /**
     * AI模型配置表列表
     * @param aiModelListBo AI模型配置表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "AI模型配置表列表")
    public R<PageUtils<AiModelListVo>> list(@Parameter(description = "AI模型配置表列表查询参数", required = true) @RequestBody AiModelListBo aiModelListBo){

        return aiModelLogic.queryPage(aiModelListBo);
    }


    /**
     * AI模型配置表信息
     * @param id AI模型配置表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "AI模型配置表信息")
    public R<AiModelInfoVo> info(@Parameter(description = "AI模型配置表id", required = true) @RequestParam("id") Long id){

        return aiModelLogic.info(id);
    }

    /**
     * 新增AI模型配置表
     * @param aiModelBo AI模型配置表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增AI模型配置表")
    public R<String> save(@Parameter(description = "AI模型配置表对象", required = true) @RequestBody AiModelBo aiModelBo){

        return aiModelLogic.save(aiModelBo);
    }

    /**
     * 修改AI模型配置表
     * @param aiModelBo AI模型配置表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改AI模型配置表")
    public R<String> update(@Parameter(description = "AI模型配置表对象", required = true) @RequestBody AiModelBo aiModelBo){

        return aiModelLogic.update(aiModelBo);
    }

    /**
     * 删除AI模型配置表
     * @param id AI模型配置表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除AI模型配置表")
    public R<String> delete(@Parameter(description = "AI模型配置表id", required = true) @RequestParam("id") Long id){

        return aiModelLogic.delete(id);
    }

}
