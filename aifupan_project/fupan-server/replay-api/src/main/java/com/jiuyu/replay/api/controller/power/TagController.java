package com.jiuyu.replay.api.controller.power;

import com.jiuyu.replay.api.logic.power.TagLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.TagBo;
import com.jiuyu.replay.power.bo.TagListBo;
import com.jiuyu.replay.power.vo.TagInfoVo;
import com.jiuyu.replay.power.vo.TagListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 用户标签
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-20 10:21:25
 */
@RestController
@CrossOrigin
@RequestMapping("replay/tag")
@Tag(name = "用户标签")
public class TagController {

    @Resource
    private TagLogic tagLogic;

    /**
     * 用户标签列表
     * @param tagListBo 用户标签列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "用户标签列表")
    public R<PageUtils<TagListVo>> list(@Parameter(description = "用户标签列表查询参数", required = true) @RequestBody TagListBo tagListBo){

        return tagLogic.queryPage(tagListBo);
    }


    /**
     * 用户标签信息
     * @param id 用户标签id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "用户标签信息")
    public R<TagInfoVo> info(@Parameter(description = "用户标签id", required = true) @RequestParam("id") Long id){

        return tagLogic.info(id);
    }

    /**
     * 新增用户标签
     * @param tagBo 用户标签对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增用户标签")
    public R<String> save(@Parameter(description = "用户标签对象", required = true) @RequestBody TagBo tagBo){

        return tagLogic.save(tagBo);
    }

    /**
     * 修改用户标签
     * @param tagBo 用户标签对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改用户标签")
    public R<String> update(@Parameter(description = "用户标签对象", required = true) @RequestBody TagBo tagBo){

        return tagLogic.update(tagBo);
    }

    /**
     * 删除用户标签
     * @param id 用户标签id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除用户标签")
    public R<String> delete(@Parameter(description = "用户标签id", required = true) @RequestParam("id") Long id){

        return tagLogic.delete(id);
    }

}
