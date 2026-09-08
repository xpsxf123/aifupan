package com.jiuyu.replay.power.controller;

import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.power.bll.TenantBll;
import com.jiuyu.replay.power.vo.TenantInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/5/13 14:34
 */
@RestController
@RequestMapping("replay/power/tenant")
@AllArgsConstructor
@Tag(name = "租户配置相关的接口")
public class PowerTenantController {

    private final TenantBll tenantBll;
    private final UserFeign userFeign;

    @GetMapping("/tenantConfig")
    @Operation(summary = "获取当前用户的租户配置")
    public R<TenantInfoVo> tenantConfig() {
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        return tenantBll.info(user.getActiveTenantId());
    }

}
