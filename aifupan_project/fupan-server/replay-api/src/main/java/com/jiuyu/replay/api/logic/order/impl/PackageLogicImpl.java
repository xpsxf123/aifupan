package com.jiuyu.replay.api.logic.order.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.order.PackageLogic;
import com.jiuyu.replay.common.bll.FileBll;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.bll.PackageBll;
import com.jiuyu.replay.order.bo.CanPurchasePackageBo;
import com.jiuyu.replay.order.bo.PackageListBo;
import com.jiuyu.replay.order.bo.PackageSaveBo;
import com.jiuyu.replay.order.vo.CurrentFreeVersionVo;
import com.jiuyu.replay.order.vo.PackageInfoVo;
import com.jiuyu.replay.order.vo.PackageListVo;
import com.jiuyu.replay.order.vo.UserVersionOrderVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * 套餐表(用户版本)
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Service
@AllArgsConstructor
public class PackageLogicImpl implements PackageLogic {

    private final PackageBll packageBll;

    private final RedisTemplate<String, Object> redisTemplate;

    private final FileBll fileBll;
    private final OrderBll orderBll;
    private final OrderFeign orderFeign;


    @Override
    public R<PageUtils<PackageListVo>> queryPage(PackageListBo packageListBo) {

        return packageBll.queryPage(packageListBo);
    }

    @Override
    public R<PackageInfoVo> info(Long id) {

        return packageBll.info(id);
    }

    @Override
    public R<String> saveOrUpdate(PackageSaveBo packageBo) {
        packageBo.setIsGive(1);
        return packageBll.saveOrUpdate(packageBo);
    }

    @Override
    public R<String> delete(Long id) {

        return packageBll.delete(id);
    }

    @Override
    public R<List<PackageInfoVo>> canPurchasePackage(Long userId) {
        OrderInfoVo order = orderFeign.currentOrderByUserId(userId);
        RRException.isNotEmpty(order, "用户未购买套餐");
        RRException.isNotEmpty(order.getId(), "用户未购买套餐");
        // 获取用户当前的套餐等级，升级套餐只能向上升级
        Integer level = order.getLevel();
        CanPurchasePackageBo bo = new CanPurchasePackageBo();
        bo.setUserId(userId);
        bo.setMinLevel(level);
        bo.setPackageType(1);
        return packageBll.canPurchasePackage(bo);
    }

    @Override
    public R<List<PackageInfoVo>> userRenewal(Long userId) {
        // 1. 获取当前订单
        OrderInfoVo order = orderFeign.currentOrderByUserId(userId);
        RRException.isNotEmpty(order, "用户未购买套餐");
        RRException.isNotEmpty(order.getId(), "用户未购买套餐");

        int currentLevel = order.getLevel() != null ? order.getLevel() : 0;

        // 2. 获取冻结订单：level >= currentLevel 且 level > 0
        Long targetCommodityId = order.getCommodityId();
        List<OrderInfoVo> frozenOrders = orderBll.listFrozenOrderByUserIdAndMinLevel(userId, Math.max(currentLevel, 1));
        if (ObjectUtil.isNotEmpty(frozenOrders)) {
            // 3. 取最大等级的冻结订单的 commodityId
            OrderInfoVo maxLevelFrozenOrder = frozenOrders.stream()
                    .max(Comparator.comparingInt(OrderInfoVo::getLevel))
                    .orElse(null);
            if (maxLevelFrozenOrder != null && maxLevelFrozenOrder.getCommodityId() != null) {
                targetCommodityId = maxLevelFrozenOrder.getCommodityId();
                currentLevel = maxLevelFrozenOrder.getLevel();
            }
        }

        if (currentLevel <= 0) {
            return R.ok(List.of());
        }

        // 4. 调用 canPurchasePackage
        CanPurchasePackageBo bo = new CanPurchasePackageBo();
        bo.setUserId(userId);
        bo.setPackageId(targetCommodityId);
        bo.setPackageType(1);
        return packageBll.canPurchasePackage(bo);
    }

    @Override
    public R<PackageInfoVo> incrementByPackageId(Long userId) {
        OrderInfoVo order = orderFeign.currentOrderByUserId(userId);
        RRException.isNotEmpty(order, "用户未购买套餐");
        RRException.isNotEmpty(order.getCommodityId(), "用户未购买套餐");
        return packageBll.incrementByPackageId(order);
    }

    @Override
    public R<List<PackageListVo>> websiteList(Long userId) {
        if (userId == null){
            UserCacheVo user = GlobalObject.getLocalUser();
            userId = user.getId();
        }
        R<UserVersionOrderVo> userVersionOrderVoR = orderBll.userVersionOrder(userId);
        if (userVersionOrderVoR.getCode() == 0 && ObjectUtil.isNotEmpty(userVersionOrderVoR.getData())){
            UserVersionOrderVo orderVo = userVersionOrderVoR.getData();
            return packageBll.websiteList(orderVo);
        }
        return R.ok(new ArrayList<>());
    }

    @Override
    public R<String> synchronousPackage(Long id) {
        return packageBll.synchronousPackage(id);
    }

    @Override
    public R<CurrentFreeVersionVo> currentFreeVersion() {
        return packageBll.currentFreeVersion();
    }

    @Override
    public R<List<PackageListVo>> trialVersionList(Long userId) {
        return R.ok(packageBll.trialVersionList(userId));
    }
}

