package com.jiuyu.replay.order.controller;

import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.order.bll.PackageBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.bo.UpdateByPropertyNumByTenantBo;
import com.jiuyu.replay.order.bo.UpdateRpaAmountNumBo;
import com.jiuyu.replay.order.bo.UserPropertyDetailsList;
import com.jiuyu.replay.order.dto.PaidUserDto;
import com.jiuyu.replay.order.dto.UserPropertyTypeCacheDto;
import com.jiuyu.replay.order.vo.PackageInfoVo;
import com.jiuyu.replay.order.vo.SingleUserPropertyDetailsListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.Executor;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/8/6 下午3:55
 */
@Slf4j
@RestController
@RequestMapping("replay/order/userProperty")
@Tag(name = "用户资产相关的控制器")
@AllArgsConstructor
public class OrderUserPropertyController {

    private final UserPropertyBll userPropertyBll;
    private final UserFeign userFeign;
    private final RedisTemplate redisTemplate;
    private final PackageBll packageBll;
    @Resource(name = "threadPoolTaskExecutor")
    private Executor asyncOutExecutor;

    @Operation(summary = "判断当前用户资产是否够", description = "判断资产是否够")
    @GetMapping("/checkUseProperty")
    public R<Boolean> checkUseProperty(String code, Long quantity) {
        UserCacheVo userResult = ResultUtil.getUserResult(userFeign.getLocalUser());
        boolean result;
        // 检查是否是订阅资产
        boolean isSubscribe = OrderEnums.commodityTypeCode.isUserShortVideoProperty(code);
        if (isSubscribe) {
            result = userPropertyBll.checkSubscribeUseProperty(userResult.getId(), code, quantity);
        } else {
            result = userPropertyBll.checkUseProperty(userResult.getId(), code, quantity);
        }
        return R.ok(result);
    }

    @PostMapping("/updateRpaAmountNum")
    @Operation(summary = "更新巨量监控位的资产")
    public R<String> updateRpaAmountNum(@RequestBody UpdateRpaAmountNumBo bo) {
        if (bo.getNum() == null || bo.getNum() < 0) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "使用的量不能小于0");
        }
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        userPropertyBll.updateByPropertyNum(user.getId(), "rpaAmountNum", bo.getNum());
        return R.ok();
    }

    @GetMapping("/syncUserPropertyRedis")
    @Operation(summary = "根据redis同步用户资产")
    public String syncUserPropertyRedis() {
        Long size = redisTemplate.opsForSet().size("replay:sync-user-property");
        if (size == null) {
            return "size为0";
        }
        asyncOutExecutor.execute(this::syncUserProperty);
        log.info("[redis同步用户资产] 有要更新的用户资产，有{}个id", size);
        return "有要更新的用户资产，有" + size + "个id";
    }

    @PostMapping("/userPropertyDetails")
    @Operation(summary = "根据userId获取用户资产")
    public R<PageUtils<SingleUserPropertyDetailsListVo>> userPropertyDetails(@RequestBody @Validated UserPropertyDetailsList bo) {
        return R.ok(userPropertyBll.userPropertyDetails(bo));
    }

    private void syncUserProperty() {
        long time = System.currentTimeMillis();
        int i = 0;
        for (i = 0; i < 20000; i++) {
            Long userPropertyId = (Long) redisTemplate.opsForSet().randomMember("replay:sync-user-property");
            if (userPropertyId == null) {
                log.info("[redis同步用户资产] userPropertyId is null, userPropertyId = {}", userPropertyId);
                break;
            }
            try {
                packageBll.syncUserPropertyRedis(userPropertyId);
                redisTemplate.opsForSet().remove("replay:sync-user-property", userPropertyId);
            } catch (Exception ex) {
                log.error("[redis同步用户资产] 资产同步报错 循环停止 i = {}, msg = {}, id = {}", i, ex.getMessage(), userPropertyId);
                break;
            }
        }
        log.info("[redis同步用户资产] 全部完成更新用户资产， size = {}, 用时 = {}ms", i + 1, System.currentTimeMillis() - time);
    }

    @PostMapping("/updateByPropertyNumByTenant")
    @Operation(summary = "设置资产")
    public R<PackageInfoVo> updateByPropertyNumByTenant(@Validated @RequestBody UpdateByPropertyNumByTenantBo tenant) {
        userPropertyBll.updateByPropertyNumByTenant(tenant);
        return R.ok("设置成功");
    }

    @Operation(summary = "获取用户的资产")
    @GetMapping("/getUserPropertyByTenant")
    public R<UserPropertyTypeCacheDto> getUserPropertyByTenant(@RequestParam(required = false) Long tenantId) {
        return R.ok(userPropertyBll.getUserPropertyByTenant(tenantId));
    }

    /**
     * 获取用户是否已付费
     */
    @GetMapping("/paid-user")
    public R<PaidUserDto> paidUser() {
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        if (user == null) {
            return R.ok(new PaidUserDto());
        }
        Long orderUserId = user.getParentId() != null && user.getParentId() != 0L ? user.getParentId() : user.getId();
        return R.ok(userPropertyBll.paidUser(orderUserId, user.getId(), user.getActiveTenantId()));
    }
}
