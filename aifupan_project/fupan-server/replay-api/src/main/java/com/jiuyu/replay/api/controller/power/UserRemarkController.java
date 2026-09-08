package com.jiuyu.replay.api.controller.power;

import com.jiuyu.replay.api.logic.power.UserRemarkLogic;
import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.vo.UserRemarkListVo;
import com.jiuyu.replay.power.vo.UserRemarkInfoVo;
import com.jiuyu.replay.power.bo.UserRemarkBo;
import com.jiuyu.replay.power.bo.UserRemarkListBo;


/**
 * 用户备注表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-12 15:59:28
 */
@RestController
@CrossOrigin
@RequestMapping("replay/userremark")
@Tag(name = "用户备注表（跟进记录）")
public class UserRemarkController {

    @Resource
    private UserRemarkLogic userRemarkLogic;

    /**
     * 用户备注表列表
     *
     * @param userRemarkListBo 用户备注表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "用户备注（跟进记录）表列表")
    public R<PageUtils<UserRemarkListVo>> list(@Parameter(description = "用户备注表列表查询参数", required = true) @RequestBody UserRemarkListBo userRemarkListBo) {

        return userRemarkLogic.queryPage(userRemarkListBo);
    }


    /**
     * 用户备注表信息
     *
     * @param id 用户备注表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "用户备注（跟进记录）表信息")
    public R<UserRemarkInfoVo> info(@Parameter(description = "用户备注表id", required = true) @RequestParam("id") Long id) {

        return userRemarkLogic.info(id);
    }

    /**
     * 新增用户备注表
     *
     * @param userRemarkBo 用户备注表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增用户备注（跟进记录）表")
    public R<String> save(@Parameter(description = "用户备注表对象", required = true) @Validated(Insert.class) @RequestBody UserRemarkBo userRemarkBo) {

        return userRemarkLogic.save(userRemarkBo);
    }

    /**
     * 修改用户备注表
     *
     * @param userRemarkBo 用户备注表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改用户备注（跟进记录）表")
    public R<String> update(@Parameter(description = "用户备注表对象", required = true) @Validated(Update.class) @RequestBody UserRemarkBo userRemarkBo) {

        return userRemarkLogic.update(userRemarkBo);
    }

    /**
     * 删除用户备注表
     *
     * @param id 用户备注表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除用户备注（跟进记录）表")
    public R<String> delete(@Parameter(description = "用户备注表id", required = true) @RequestParam("id") Long id) {

        return userRemarkLogic.delete(id);
    }

}
