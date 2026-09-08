package com.jiuyu.replay.api.controller.power;

import com.jiuyu.replay.api.logic.power.UserTagLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.UserTagBo;
import com.jiuyu.replay.power.bo.UserTagListBo;
import com.jiuyu.replay.power.vo.UserTagInfoVo;
import com.jiuyu.replay.power.vo.UserTagListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 用户-标签-关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-20 10:21:25
 */
@RestController
@CrossOrigin
@RequestMapping("replay/usertag")
@Tag(name = "用户-标签-关联")
public class UserTagController {

    @Resource
    private UserTagLogic userTagLogic;

    /**
     * 用户-标签-关联列表
     * @param userTagListBo 用户-标签-关联列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "用户-标签-关联列表")
    public R<PageUtils<UserTagListVo>> list(@Parameter(description = "用户-标签-关联列表查询参数", required = true) @RequestBody UserTagListBo userTagListBo){

        return userTagLogic.queryPage(userTagListBo);
    }


    /**
     * 用户-标签-关联信息
     * @param id 用户-标签-关联id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "用户-标签-关联信息")
    public R<UserTagInfoVo> info(@Parameter(description = "用户-标签-关联id", required = true) @RequestParam("id") Long id){

        return userTagLogic.info(id);
    }

    /**
     * 新增用户-标签-关联
     * @param userTagBo 用户-标签-关联对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增用户-标签-关联")
    public R<String> save(@Parameter(description = "用户-标签-关联对象", required = true) @RequestBody UserTagBo userTagBo){

        return userTagLogic.save(userTagBo);
    }

    /**
     * 修改用户-标签-关联
     * @param userTagBo 用户-标签-关联对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改用户-标签-关联")
    public R<String> update(@Parameter(description = "用户-标签-关联对象", required = true) @RequestBody UserTagBo userTagBo){

        return userTagLogic.update(userTagBo);
    }

    /**
     * 删除用户-标签-关联
     * @param id 用户-标签-关联id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除用户-标签-关联")
    public R<String> delete(@Parameter(description = "用户-标签-关联id", required = true) @RequestParam("id") Long id){

        return userTagLogic.delete(id);
    }

}
