package com.jiuyu.replay.order.controller;

import cn.hutool.core.util.ObjUtil;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.bo.ClientVersionConfigBo;
import com.jiuyu.replay.order.vo.UserClientVersionConfigVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author ：lujie
 * @date ：2026/4/7 下午3:55
 */
@Slf4j
@RestController
@RequestMapping("replay/order/clientOrder")
@Tag(name = "客户端关于版本的控制器")
@AllArgsConstructor
public class ClientOrderController {

    private final OrderBll orderBll;
    private final UserFeign userFeign;

    @PostMapping("/setClientVersionConfig")
    @Operation(summary = "纯录制版/复盘按切换")
    public R<String> setClientVersionConfig(@Validated @RequestBody ClientVersionConfigBo clientVersionConfigBo) {
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        // 判断是否是子账号，如果是子账号就返回错误
        if (!ObjUtil.equal(UserEnums.userType.CLIENT_USER.getCode(), user.getUserType())) {
            return R.error(StatusCode.OPERATION_EX.getCode(), "账号类型错误，只能爱复盘主账号切换");
        }

        // 获取userId
        clientVersionConfigBo.setUserId(user.getId());
        orderBll.setClientVersionConfig(clientVersionConfigBo);
        return R.ok();
    }

    @GetMapping("/userClientVersionConfig")
    @Operation(summary = "获取当前用户的客户端版本")
    public R<UserClientVersionConfigVo> userClientVersionConfig() {
        Long currentUserParentId = userFeign.getCurrentUserParentId();
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());

        UserClientVersionConfigVo result = orderBll.userClientVersionConfig(currentUserParentId);

        if (!ObjUtil.equal(currentUserParentId, user.getId())) {
            result.setIsVersionSelect(OrderEnums.versionSelect.NOT_SELECT.getCode());
        }

        result.setUserType(user.getUserType());

        return R.ok(result);
    }


}
