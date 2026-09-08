package com.jiuyu.replay.api.controller.system;

import java.util.List;

import com.jiuyu.replay.api.logic.system.LoginRotateImageLogic;
import com.jiuyu.replay.system.bo.LoginRotateImageBo;
import com.jiuyu.replay.system.bo.LoginRotateImageListBo;
import com.jiuyu.replay.system.vo.LoginRotateImageInfoVo;
import com.jiuyu.replay.system.vo.LoginRotateImageListVo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.generic.vo.common.R;





/**
 * 客户端登录页轮播图
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 15:27:24
 */
@RestController
@CrossOrigin
@RequestMapping("replay/loginrotateimage")
@Tag(name = "客户端登录页轮播图")
public class LoginRotateImageController {

    @Resource
    private LoginRotateImageLogic loginRotateImageLogic;

    /**
     * 客户端登录页轮播图列表
     * @param loginRotateImageListBo 客户端登录页轮播图列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "客户端登录页轮播图列表-分页")
    public R<PageUtils<LoginRotateImageListVo>> list(@Parameter(description = "客户端登录页轮播图列表查询参数", required = true) @RequestBody LoginRotateImageListBo loginRotateImageListBo){

        return loginRotateImageLogic.queryPage(loginRotateImageListBo);
    }


    /**
     * 客户端登录页轮播图列表
     * @param
     * @return
     */
    @PostMapping("/noPage")
    @Operation(summary = "客户端登录页轮播图列表")
    public R<List<LoginRotateImageListVo>> noPage(){

        return loginRotateImageLogic.noPage();
    }


    /**
     * 客户端登录页轮播图信息
     * @param id 客户端登录页轮播图id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "客户端登录页轮播图信息")
    public R<LoginRotateImageInfoVo> info(@Parameter(description = "客户端登录页轮播图id", required = true) @RequestParam("id") Long id){

        return loginRotateImageLogic.info(id);
    }

    /**
     * 新增客户端登录页轮播图
     * @param loginRotateImageBo 客户端登录页轮播图对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增客户端登录页轮播图")
    public R<String> save(@Parameter(description = "客户端登录页轮播图对象", required = true) @RequestBody LoginRotateImageBo loginRotateImageBo){

        return loginRotateImageLogic.save(loginRotateImageBo);
    }

    /**
     * 修改客户端登录页轮播图
     * @param loginRotateImageBo 客户端登录页轮播图对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改客户端登录页轮播图")
    public R<String> update(@Parameter(description = "客户端登录页轮播图对象", required = true) @RequestBody LoginRotateImageBo loginRotateImageBo){

        return loginRotateImageLogic.update(loginRotateImageBo);
    }

    /**
     * 删除客户端登录页轮播图
     * @param id 客户端登录页轮播图id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除客户端登录页轮播图")
    public R<String> delete(@Parameter(description = "客户端登录页轮播图id", required = true) @RequestParam("id") Long id){

        return loginRotateImageLogic.delete(id);
    }

}
