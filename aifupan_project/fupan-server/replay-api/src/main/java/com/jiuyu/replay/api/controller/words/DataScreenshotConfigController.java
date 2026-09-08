package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.DataScreenshotConfigLogic;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.vo.DataScreenshotConfigListVo;
import com.jiuyu.replay.words.vo.DataScreenshotConfigInfoVo;
import com.jiuyu.replay.words.bo.DataScreenshotConfigBo;
import com.jiuyu.replay.words.bo.DataScreenshotConfigListBo;



/**
 * 数据截图配置
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@RestController
@CrossOrigin
@RequestMapping("replay/dataScreenshotConfig")
@Tag(name = "数据截图配置")
public class DataScreenshotConfigController {

    @Resource
    private DataScreenshotConfigLogic dataScreenshotConfigLogic;

    /**
     * 数据截图配置列表
     * @param dataScreenshotConfigListBo 数据截图配置列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "数据截图配置列表")
    public R<PageUtils<DataScreenshotConfigListVo>> list(@Parameter(description = "数据截图配置列表查询参数", required = true) @RequestBody DataScreenshotConfigListBo dataScreenshotConfigListBo){

        return dataScreenshotConfigLogic.queryPage(dataScreenshotConfigListBo);
    }


    /**
     * 数据截图配置信息
     * @param id 数据截图配置id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "数据截图配置信息")
    public R<DataScreenshotConfigInfoVo> info(@Parameter(description = "数据截图配置id", required = true) @RequestParam("id") Long id){

        return dataScreenshotConfigLogic.info(id);
    }

    /**
     * 新增数据截图配置
     * @param dataScreenshotConfigBo 数据截图配置对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增数据截图配置")
    public R<String> save(@Parameter(description = "数据截图配置对象", required = true) @RequestBody DataScreenshotConfigBo dataScreenshotConfigBo){

        return dataScreenshotConfigLogic.save(dataScreenshotConfigBo);
    }

    /**
     * 修改数据截图配置
     * @param dataScreenshotConfigBo 数据截图配置对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改数据截图配置")
    public R<String> update(@Parameter(description = "数据截图配置对象", required = true) @RequestBody DataScreenshotConfigBo dataScreenshotConfigBo){

        return dataScreenshotConfigLogic.update(dataScreenshotConfigBo);
    }

    /**
     * 删除数据截图配置
     * @param id 数据截图配置id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除数据截图配置")
    public R<String> delete(@Parameter(description = "数据截图配置id", required = true) @RequestParam("id") Long id){

        return dataScreenshotConfigLogic.delete(id);
    }

}
