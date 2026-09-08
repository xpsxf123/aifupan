package com.jiuyu.replay.api.controller.power;

import com.jiuyu.replay.api.logic.power.UserDetailsLogic;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.UserDetailsBo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * 用户详情表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:41
 */
@RestController
@CrossOrigin
@RequestMapping("replay/userdetails")
@Tag(name = "用户详情")
public class UserDetailsController {
    @Resource
    UserDetailsLogic userDetailsLogic;

    /**
     * 客户端保存或修改用户详情接口
     * @param userDetailsBo
     * @return
     */
    @PostMapping("/saveUserDetails")
    @Operation(summary = "客户端保存或修改用户详情接口")
    public R<String> saveUserDetails(@RequestBody UserDetailsBo userDetailsBo){
        return userDetailsLogic.saveUserDetails(userDetailsBo);
    }


}
