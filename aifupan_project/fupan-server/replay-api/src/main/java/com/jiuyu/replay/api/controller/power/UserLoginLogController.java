package com.jiuyu.replay.api.controller.power;

import com.jiuyu.replay.api.logic.power.UserLoginLogLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.UserLoginLogBo;
import com.jiuyu.replay.power.bo.UserLoginLogListBo;
import com.jiuyu.replay.power.vo.UserLoginLogInfoVo;
import com.jiuyu.replay.power.vo.UserLoginLogListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 用户登录日志
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-15 18:48:54
 */
@RestController
@CrossOrigin
@RequestMapping("replay/userloginlog")
@Tag(name = "用户登录日志")
public class UserLoginLogController {

    @Resource
    private UserLoginLogLogic userLoginLogLogic;

    /**
     * 用户登录日志列表
     * @param userLoginLogListBo 用户登录日志列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "用户登录日志列表")
    public R<PageUtils<UserLoginLogListVo>> list(@Parameter(description = "用户登录日志列表查询参数", required = true) @RequestBody UserLoginLogListBo userLoginLogListBo){

        return userLoginLogLogic.queryPage(userLoginLogListBo);
    }


    /**
     * 用户登录日志信息
     * @param id 用户登录日志id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "用户登录日志信息")
    public R<UserLoginLogInfoVo> info(@Parameter(description = "用户登录日志id", required = true) @RequestParam("id") Long id){

        return userLoginLogLogic.info(id);
    }

    /**
     * 新增用户登录日志
     * @param userLoginLogBo 用户登录日志对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增用户登录日志")
    public R<String> save(@Parameter(description = "用户登录日志对象", required = true) @RequestBody UserLoginLogBo userLoginLogBo){

        return userLoginLogLogic.save(userLoginLogBo);
    }

    /**
     * 修改用户登录日志
     * @param userLoginLogBo 用户登录日志对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改用户登录日志")
    public R<String> update(@Parameter(description = "用户登录日志对象", required = true) @RequestBody UserLoginLogBo userLoginLogBo){

        return userLoginLogLogic.update(userLoginLogBo);
    }

    /**
     * 删除用户登录日志
     * @param id 用户登录日志id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除用户登录日志")
    public R<String> delete(@Parameter(description = "用户登录日志id", required = true) @RequestParam("id") Long id){

        return userLoginLogLogic.delete(id);
    }

}
