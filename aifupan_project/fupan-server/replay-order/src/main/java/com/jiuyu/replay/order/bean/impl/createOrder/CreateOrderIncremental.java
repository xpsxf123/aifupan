package com.jiuyu.replay.order.bean.impl.createOrder;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.utils.ApplicationContextUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.order.bean.impl.CreateOrderBeanAbstract;
import com.jiuyu.replay.order.bo.CreateOrderBo;
import com.jiuyu.replay.order.bo.OrderBo;
import com.jiuyu.replay.order.bo.OrderDetailBo;
import com.jiuyu.replay.order.producer.CommodityPriceProducer;
import com.jiuyu.replay.order.producer.CommodityProducer;
import com.jiuyu.replay.order.producer.CommodityTypeProducer;
import com.jiuyu.replay.order.producer.IncrementProducer;
import com.jiuyu.replay.order.producer.impl.CommodityPriceProducerImpl;
import com.jiuyu.replay.order.producer.impl.CommodityProducerImpl;
import com.jiuyu.replay.order.producer.impl.CommodityTypeProducerImpl;
import com.jiuyu.replay.order.producer.impl.IncrementProducerImpl;
import com.jiuyu.replay.order.vo.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

/**
 * 创建增量订单
 */
public class CreateOrderIncremental extends CreateOrderBeanAbstract {

    private CommodityTypeProducer commodityTypeProducer;

    private CommodityProducer commodityProducer;

    private CommodityPriceProducer commodityPriceProducer;

    private IncrementProducer incrementProducer;

    /**
     * 构造函数
     */
    public CreateOrderIncremental() {
        this.commodityTypeProducer = ApplicationContextUtil.getBean(CommodityTypeProducerImpl.class);
        this.commodityProducer = ApplicationContextUtil.getBean(CommodityProducerImpl.class);
        this.commodityPriceProducer = ApplicationContextUtil.getBean(CommodityPriceProducerImpl.class);
        this.incrementProducer = ApplicationContextUtil.getBean(IncrementProducerImpl.class);
    }

    @Override
    public OrderBo saveOrder(CreateOrderBo createOrderBo) {
        if (ObjectUtil.isEmpty(createOrderBo.getCommodityId())) RRException.create("增量包id不能为空");
        if (ObjectUtil.isEmpty(createOrderBo.getUserId())) RRException.create("用户id不能为空");
        // 1.查询增量包信息
        IncrementInfoVo increment = getIncrementInfoVo(createOrderBo.getCommodityId());
        RRException.isNotEmpty(increment, "增量包不存在");
        // 1.1 查询商品信息
        CommodityInfoVo commodity = increment.getCommodity();
        RRException.isNotEmpty(commodity, "商品不存在");
        // 1.2 查询商品价格
        CommodityPriceVo priceVo = increment.getCommodityPrice();
        RRException.isNotEmpty(priceVo, "增量包价格不存在");
        CommodityTypeInfoVo commodityTypeInfoVo = commodityTypeProducer.info(commodity.getCommodityTypeId());
        RRException.isNotEmpty(commodityTypeInfoVo, "商品类型不存在");


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
        result.setCommodityId(increment.getId());
        // 设置商品的名称
        result.setCommodityName(commodity.getName());
        // 设置订单的标题，这里直接使用商品名称
        result.setTitle(commodity.getName());
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
        result.setOriginalPrice(priceVo.getOriginalPrice());
        // 设置订单的折扣
        result.setDiscount(increment.getDiscount());
        // 设置订单的折扣率
        result.setDiscountRate(createOrderBo.getDiscountRate());
        // 计算订单价格
        result.setRealPrice(increment.getRealPrice().intValue());
        // 计算订单的总价
        result.setTotalPrice((result.getRealPrice() - createOrderBo.getDiscountRate()) * result.getQuantity());
        // 设置订单的有效期
        result.setExpiration(priceVo.getValidityNum());
        // 设置订单的有效期单位
        result.setExpirationUnit(priceVo.getValidityUnit());
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
        result.setTrialOrder(priceVo.getTrialVersion());

        // 2.1 封装订单详情
        ArrayList<OrderDetailBo> orderDetailList = new ArrayList<>();
        OrderDetailBo e = new OrderDetailBo();
        // 为订单详情生成一个唯一的ID
        e.setId(SnowflakeManager.nextValue());
        // 设置订单详情的订单ID
        e.setOrderId(result.getId());
        // 设置订单详情的商品ID
        e.setCommodityId(increment.getCommodityId());
        // 设置订单详情的商品名称
        e.setCommodityName(commodity.getName());
        // 设置商品的类型ID
        e.setCommodityTypeId(commodity.getCommodityTypeId());
        // 设置订单详情的商品类型名称
        e.setCommodityTypeName(commodityTypeInfoVo.getName());
        // 设置商品的类型代码
        e.setCommodityTypeCode(commodityTypeInfoVo.getCode());
        // 设置商品的类型单位
        e.setCommodityTypeUnit(commodityTypeInfoVo.getUnit());
        // 设置商品的类型是否重置
        e.setCommodityTypeReset(commodityTypeInfoVo.getIsReset());
        // 设置商品的总数量
        e.setTotalNumber(commodity.getNumber());
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

    private IncrementInfoVo getIncrementInfoVo(Long id) {
        IncrementInfoVo increment = incrementProducer.info(id);
        if (increment != null) {

            // 1.1 查询商品信息
            CommodityInfoVo commodity = commodityProducer.info(increment.getCommodityId());
            // 1.2 查询商品价格
            CommodityPriceInfoVo priceVo = commodityPriceProducer.info(increment.getCommodityPriceId());
            increment.setCommodityPrice(priceVo);
            increment.setCommodity(commodity);
            return increment;
        }
        CommodityPriceInfoVo priceVo = commodityPriceProducer.info(id);
        CommodityInfoVo commodity = commodityProducer.info(priceVo.getCommodityId());
        increment = new IncrementInfoVo();
        increment.setId(id);
        increment.setCommodityPrice(priceVo);
        increment.setCommodity(commodity);
        increment.setCommodityId(priceVo.getCommodityId());
        increment.setCommodityPriceId(priceVo.getId());
        increment.setPackageId(increment.getPackageId());
        increment.setDiscount(BigDecimal.valueOf(1));
        increment.setRealPrice(priceVo.getOriginalPrice() == null ? BigDecimal.ZERO : BigDecimal.valueOf(priceVo.getOriginalPrice()));
        increment.setStatus(OrderEnums.status.ENABLE.getCode());

        return increment;
    }

    @Override
    public void checkOrder(CreateOrderBo createOrderBo) {
        super.checkOrder(createOrderBo);
        if (ObjectUtil.isEmpty(createOrderBo.getCommodityId())) RRException.create("商品不能为空");
    }
}
