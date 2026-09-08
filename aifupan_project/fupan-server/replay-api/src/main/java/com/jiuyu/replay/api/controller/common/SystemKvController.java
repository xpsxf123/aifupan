package com.jiuyu.replay.api.controller.common;

import com.jiuyu.replay.api.logic.common.SystemKvLogic;
import com.jiuyu.replay.common.bo.ImgConfigBo;
import com.jiuyu.replay.common.bo.SystemKvBo;
import com.jiuyu.replay.common.bo.SystemKvListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.ImgConfigVo;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.common.vo.SystemKvListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 系统配置的键值对
 *
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
@RestController
@CrossOrigin
@RequestMapping("replay/systemkv")
@Tag(name = "系统配置的键值对")
public class SystemKvController {

    @Resource
    private SystemKvLogic systemKvLogic;

    /**
     * 修改图片配置
     * @param imgConfigBo 图片配置信息
     * @return
     */
    @PostMapping("/updateImgConfig")
    @Operation(summary = "修改图片配置")
    public R<String> updateImgConfig(@RequestBody ImgConfigBo imgConfigBo){

        return systemKvLogic.updateImgConfig(imgConfigBo);
    }

    /**
     * 获取图片配置
     * @return
     */
    @GetMapping("/getImgConfig")
    @Operation(summary = "获取图片配置")
    public R<ImgConfigVo> getImgConfig(){

        return systemKvLogic.getImgConfig();
    }

    /**
     * 系统配置的键值对列表
     * @param systemKvListBo 系统配置的键值对列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "系统配置的键值对列表")
    public R<PageUtils<SystemKvListVo>> list(@Parameter(description = "系统配置的键值对列表查询参数", required = true) @RequestBody SystemKvListBo systemKvListBo){

        return systemKvLogic.queryPage(systemKvListBo);
    }


    /**
     * 系统配置的键值对信息
     * @param id 系统配置的键值对id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "系统配置的键值对信息")
    public R<SystemKvInfoVo> info(@Parameter(description = "系统配置的键值对id", required = true) @RequestParam("id") Long id){

        return systemKvLogic.info(id);
    }

    /**
     * 新增系统配置的键值对
     * @param systemKvBo 系统配置的键值对对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增系统配置的键值对")
    public R<String> save(@Parameter(description = "系统配置的键值对对象", required = true) @RequestBody SystemKvBo systemKvBo){

        return systemKvLogic.save(systemKvBo);
    }

    /**
     * 修改系统配置的键值对
     * @param systemKvBo 系统配置的键值对对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改系统配置的键值对")
    public R<String> update(@Parameter(description = "系统配置的键值对对象", required = true) @RequestBody SystemKvBo systemKvBo){

        return systemKvLogic.update(systemKvBo);
    }

    /**
     * 删除系统配置的键值对
     * @param id 系统配置的键值对id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除系统配置的键值对")
    public R<String> delete(@Parameter(description = "系统配置的键值对id", required = true) @RequestParam("id") Long id){

        return systemKvLogic.delete(id);
    }

    @GetMapping("/getByKey")
    @Operation(summary = "根据key获取系统配置的键值对信息")
    public R<SystemKvInfoVo> getByKey(@Parameter(description = "系统配置的键值对key", required = true) @RequestParam("key") String key) {
        return systemKvLogic.getByKey(key);
    }

}
