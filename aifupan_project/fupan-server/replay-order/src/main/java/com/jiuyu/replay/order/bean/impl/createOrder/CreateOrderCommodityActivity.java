package com.jiuyu.replay.order.bean.impl.createOrder;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.common.utils.ApplicationContextUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.order.bean.impl.CreateOrderBeanAbstract;
import com.jiuyu.replay.order.bo.CreateOrderBo;
import com.jiuyu.replay.order.bo.OrderBo;
import com.jiuyu.replay.order.bo.OrderDetailBo;
import com.jiuyu.replay.order.producer.CommodityTypeProducer;
import com.jiuyu.replay.order.producer.impl.CommodityTypeProducerImpl;
import com.jiuyu.replay.order.vo.CommodityTypeInfoVo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/9 下午5:37
 */
public class CreateOrderCommodityActivity extends CreateOrderBeanAbstract {

    private CommodityTypeProducer commodityTypeProducer;

    public CreateOrderCommodityActivity() {
        this.commodityTypeProducer = ApplicationContextUtil.getBean(CommodityTypeProducerImpl.class);
    }

    @Override
    public OrderBo saveOrder(CreateOrderBo createOrderBo) {
        if (ObjectUtil.isEmpty(createOrderBo.getCommodityId())) RRException.create("活动记录id不能为空");
        // 1.2 查询商品价格
//        CommodityPriceInfoVo priceVo = commodityPriceProducer.info(increment.getCommodityPriceId());
        CommodityTypeInfoVo commodityTypeInfoVo = commodityTypeProducer.info(createOrderBo.getActivityCommodityBo().getId());

        // 2.0 封装订单信息
        // 创建一个新的订单业务对象
        OrderBo result = new OrderBo();
        // 为订单生成一个唯一的ID
        result.setId(SnowflakeManager.nextValue());
        // 设置订单的用户ID
        result.setUserId(createOrderBo.getUserId());
        // 设置订单的用户名
        result.setUserName(createOrderBo.getUserName());
        // 设置商品的ID
        result.setCommodityId(createOrderBo.getCommodityId());
        // 设置商品的名称
        result.setCommodityName(commodityTypeInfoVo.getName());
        // 设置订单的标题，这里直接使用商品名称
        List<String> strName = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(createOrderBo.getTitle())){
            strName.add(createOrderBo.getTitle());
        }
        if (ObjectUtil.isNotEmpty(commodityTypeInfoVo.getName())){
            strName.add(commodityTypeInfoVo.getName());
        }
        result.setTitle(StrUtil.join("-", strName));
        // 设置订单的状态为0，表示未支付
        result.setStatus(0);
        // 设置订单的类型
        result.setOrderType(createOrderBo.getOrderType());
        // 设置商品的类型为1，这里可能有特定的含义，比如1代表某种特定类型的商品
        result.setCommodityType(createOrderBo.getCommodityType());
        // 设置等级为套餐对象中的等级
        result.setLevel(0);
        // 设置订单的数量为1
        result.setQuantity(1);
        // 设置订单的原价
        result.setOriginalPrice(0);
        // 设置订单的折扣
        result.setDiscount(new BigDecimal(0));
        // 设置订单的折扣率
        result.setDiscountRate(createOrderBo.getDiscountRate());
        // 计算订单价格
        result.setRealPrice(0);
        // 计算订单的总价
        result.setTotalPrice(0);
        // 设置订单的有效期
        result.setExpiration(createOrderBo.getActivityCommodityBo().getValidityNum());
        // 设置订单的有效期单位
        result.setExpirationUnit(createOrderBo.getActivityCommodityBo().getValidityUnit());
        // 初始化支付日期为null
        result.setPayDate(null);
        // 初始化结束日期为null
        result.setEndDate(null);
        // 设置订单的升级前状态
        result.setBeforeUpgrading(createOrderBo.getBeforeUpgrading());
        // 初始化升级后状态为null
        result.setAfterUpgrading(null);
        // 设置订单的来源
        result.setSource(createOrderBo.getSource());
        // 设置订单的创建日期和更新日期为当前日期
        Date date = new Date();
        result.setCreateDate(date);
        result.setUpdateDate(date);
        // 设置订单的删除状态为0，表示未删除
        result.setIsDeleted(0);
        // 是否是试用订单
        result.setTrialOrder(1);

        // 2.1 封装订单详情
        ArrayList<OrderDetailBo> orderDetailList = new ArrayList<>();
        OrderDetailBo e = new OrderDetailBo();
        // 为订单详情生成一个唯一的ID
        e.setId(SnowflakeManager.nextValue());
        // 设置订单详情的订单ID
        e.setOrderId(result.getId());
        // 设置订单详情的商品ID
        e.setCommodityId(commodityTypeInfoVo.getId());
        // 设置订单详情的商品名称
        e.setCommodityName(commodityTypeInfoVo.getName());
        // 设置商品的类型ID
        e.setCommodityTypeId(commodityTypeInfoVo.getId());
        // 设置订单详情的商品类型名称
        e.setCommodityTypeName(commodityTypeInfoVo.getName());
        // 设置商品的类型代码
        e.setCommodityTypeCode(commodityTypeInfoVo.getCode());
        // 设置商品的类型单位
        e.setCommodityTypeUnit(commodityTypeInfoVo.getUnit());
        // 设置商品的类型是否重置
        e.setCommodityTypeReset(commodityTypeInfoVo.getIsReset());
        // 设置商品的总数量
        e.setTotalNumber(createOrderBo.getActivityCommodityBo().getCommodityNumber());
        // 设置开始日期为当前日期
        e.setStartDate(null);
        // 设置重置数量
        e.setResetNum(commodityTypeInfoVo.getResetNum());
        // 设置重置单位
        e.setResetUnit(commodityTypeInfoVo.getResetUnit());
        // 初始化下一次重置日期为null
        e.setNextReset(null);
        // 初始化过期日期为null
        e.setExpirationDate(null);
        // 设置订单详情的状态为0，表示正常
        e.setStatus(0);
        // 设置订单详情的创建日期和更新日期为当前日期
        e.setCreateDate(date);
        e.setUpdateDate(date);
        // 设置订单详情的删除状态为0，表示未删除
        e.setIsDeleted(0);
        // 将订单详情添加到列表中
        orderDetailList.add(e);
        // 设置订单的订单详情列表
        result.setOrderDetailList(orderDetailList);
        // 返回创建好的订单对象
        return result;
    }

    @Override
    public void checkOrder(CreateOrderBo createOrderBo) {
        super.checkOrder(createOrderBo);

        RRException.isNotEmpty(createOrderBo.getActivityCommodityBo(), "活动商品不能为空");
    }
}
