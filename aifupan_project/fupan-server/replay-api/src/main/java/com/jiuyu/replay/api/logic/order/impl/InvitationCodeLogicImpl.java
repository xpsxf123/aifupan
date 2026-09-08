package com.jiuyu.replay.api.logic.order.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.order.InvitationCodeLogic;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.order.bll.InvitationCodeBatchBll;
import com.jiuyu.replay.order.bll.InvitationCodeBll;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.bll.PackageBll;
import com.jiuyu.replay.order.bo.CreateOrderBo;
import com.jiuyu.replay.order.bo.InvitationCodeBo;
import com.jiuyu.replay.order.bo.InvitationCodeListBo;
import com.jiuyu.replay.order.entity.InvitationUsageRecordEntity;
import com.jiuyu.replay.order.vo.*;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserInfoVo;
import com.jiuyu.replay.power.vo.UserListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;


/**
 * 邀请码
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Service
public class InvitationCodeLogicImpl implements InvitationCodeLogic {

    @Resource
    private InvitationCodeBll invitationCodeBll;
    @Resource
    private InvitationCodeBatchBll invitationCodeBatchBll;

    @Resource
    private UserBll userBll;

    @Resource
    private OrderBll orderBll;
    @Autowired
    private PackageBll packageBll;


    @Override
    public R<PageUtils<InvitationCodeListVo>> queryPage(InvitationCodeListBo invitationCodeListBo) {

        R<PageUtils<InvitationCodeListVo>> pageUtilsR = invitationCodeBll.queryPage(invitationCodeListBo);
        // 封装邀请码使用人的名字
        if (pageUtilsR.getCode() == 0 && ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())){
            List<InvitationCodeListVo> data = pageUtilsR.getData().getList();
            if(data != null && !data.isEmpty()) {
                List<Long> userIds = data.stream().map(InvitationCodeVo::getUserId).filter(userId -> !userId.equals(0L)).toList();
                if(!userIds.isEmpty()) {
                    R<List<UserListVo>> listR1 = this.userBll.listByIds(userIds);
                    List<UserListVo> userListVoList = listR1.getData();
                    if(userListVoList != null && userListVoList.size() > 0) {
                        for (InvitationCodeListVo invitationCodeListVo : data) {
                            if(!invitationCodeListVo.getUserId().equals(0L)) {
                                for (UserListVo userListVo : userListVoList) {
                                    if(invitationCodeListVo.getUserId().equals(userListVo.getId())) {
                                        invitationCodeListVo.setUserName(userListVo.getNickName());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return pageUtilsR;
    }

    @Override
    public R<InvitationCodeInfoVo> info(Long id) {

        return invitationCodeBll.info(id);
    }

    @Override
    public R<String> save(InvitationCodeBo invitationCodeBo) {

        return invitationCodeBll.save(invitationCodeBo);
    }

    @Override
    public R<String> update(InvitationCodeBo invitationCodeBo) {

        return invitationCodeBll.update(invitationCodeBo);
    }

    @Override
    public R<String> delete(Long id) {

        return invitationCodeBll.delete(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> exchange(String code) {
        UserCacheVo user = GlobalObject.getLocalUser();

        // 检查是否能用邀请码
        R<InvitationCodeInfoVo> invitationCodeInfoVoR = this.invitationCodeBll.checkInvitationCode(user.getId(), code);
        if(invitationCodeInfoVoR.getCode() == 0) {
            InvitationCodeInfoVo data = invitationCodeInfoVoR.getData();
            if(data.getInvitationCodeBatchVo().getType() == 2) {
                // 判断是否能使用激活码
                orderBll.isUseActivationCode(user.getId());
            }
            CreateOrderBo orderBo = createOrderBo(data);
            // 获取是否是免费
            InvitationCodeBatchInfoVo batchInfoVo = ResultUtil.getResult(invitationCodeBatchBll.info(data.getBatchId()));
            if (batchInfoVo != null) {
                orderBo.getPriceVo().setTrialVersion(batchInfoVo.getIsGratis());
            }
            // 下单
            CreateOrderVo order = orderBll.createOrder(orderBo);
            // 完成订单-更新数据库
            // 手动的单，直接订单完成接口
            if (ObjectUtil.isNotEmpty(order)){
                orderBll.successOrder(order.getOrderId(), orderBo.getOrderType() != 3);
            }

            // 跟新邀请码状态-是否是无限次
            if (data.getInvitationCodeBatchVo().getIsInfinite() == 0){
                invitationCodeBll.useInvitation(user.getId(), order.getOrderId(), data.getId());
            }

            // 添加邀请码使用记录
            InvitationUsageRecordEntity entity = new InvitationUsageRecordEntity();
            entity.setInvitationCodeBatchId(data.getInvitationCodeBatchVo().getId());
            entity.setInvitationCodeId(data.getId());
            entity.setInvitationCode(data.getCode());
            entity.setTenantId(user.getActiveTenantId());
            entity.setUserId(user.getId());
            entity.setOrderId(order.getOrderId());
            entity.setCreateId(user.getId());
            entity.setUpdateId(user.getId());
            invitationCodeBll.saveInvitationUsageRecord(entity);
        }

        return R.error(invitationCodeInfoVoR.getCode(), invitationCodeInfoVoR.getMsg());
    }



    /**
     * 构建下单的参数
     * @param data
     * @return
     */
    public CreateOrderBo createOrderBo(InvitationCodeInfoVo data){


        UserCacheVo user = GlobalObject.getLocalUser();
        CreateOrderBo result = new CreateOrderBo();
        result.setUserId(user.getId());
        R<UserInfoVo> info = userBll.info(result.getUserId(), false);
        if (info.getCode() == 0 && info.getData() != null){
            result.setUserName(info.getData().getNickName());
        }

        // 价格
        CommodityPriceInfoVo priceVo = new CommodityPriceInfoVo();
        priceVo.setValidityNum(data.getInvitationCodeBatchVo().getCommodityValidityNum());
        priceVo.setValidityUnit(data.getInvitationCodeBatchVo().getCommodityValidityUnit());
        priceVo.setDiscount(BigDecimal.valueOf(1));
        priceVo.setOriginalPrice(data.getInvitationCodeBatchVo().getCommodityRealPrice());
        priceVo.setRealPrice(data.getInvitationCodeBatchVo().getCommodityRealPrice());
        result.setPriceVo(priceVo);

        result.setSource(2);
        result.setPayType(-1);
        // 设置类型
        if (data.getInvitationCodeBatchVo().getType() == 2){
            R<OrderInfoVo> currentOrder = orderBll.currentOrderByUserId(user.getId());
            result.setBeforeUpgrading(currentOrder.getData().getId());
            result.setCommodityType(1);
            result.setOrderType(0);
        }else{
            if (data.getInvitationCodeBatchVo().getCommodityType() == 1){
                // 这里是活动的订单
                result.setCommodityType(2);
                result.setOrderType(5);
            }else{
                R<OrderInfoVo> currentOrder = orderBll.currentOrderByUserId(user.getId());
                // 判断当前用户版本的level是否大于0，如果是-1或者是0，订单就是升级订单
                if (currentOrder.getData().getLevel() <= 0){
                    result.setBeforeUpgrading(currentOrder.getData().getId());
                    result.setCommodityType(1);
                    result.setOrderType(2);
                }else{
                    // 如果大于0，用户版本等于邀请码版本，就是续费订单，否则就是不给下单
                    R<PackageInfoVo> bllBtId = packageBll.getBtId(data.getInvitationCodeBatchVo().getCommodityId());
                    if (bllBtId.getCode() != 0 || ObjectUtil.isEmpty(bllBtId.getData())) RRException.create("商品查询失败");
                    PackageInfoVo packageBo = bllBtId.getData();

                    if (Objects.equals(currentOrder.getData().getLevel(), packageBo.getLevel())){
                        result.setBeforeUpgrading(currentOrder.getData().getId());
                        result.setCommodityType(1);
                        result.setOrderType(3);
                    }else{
                        RRException.create("邀请码版本和用户版本不一致，不能使用");
                    }
                }
            }
        }
        result.setCommodityId(data.getInvitationCodeBatchVo().getCommodityId());
        result.setDiscountRate(0);
        result.setSource(2);
        return result;
    }

    /**
     * 邀请码导出
     * @param invitationCodeBo
     * @return
     */
    @Override
    public R<List<InvitationCodeListVo>> exportInvitation(List<InvitationCodeBo> invitationCodeBo) {
        return invitationCodeBll.exportInvitation(invitationCodeBo);
    }

    @Override
    public R<String> updateIsLssued(List<Long> ids) {
        return invitationCodeBll.updateIsLssued(ids);
    }
}

