package com.jiuyu.replay.order.bean.impl.createOrder;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.common.utils.ApplicationContextUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.order.bean.impl.CreateOrderBeanAbstract;
import com.jiuyu.replay.order.bo.CreateOrderBo;
import com.jiuyu.replay.order.bo.OrderBo;
import com.jiuyu.replay.order.bo.OrderDetailBo;
import com.jiuyu.replay.order.producer.*;
import com.jiuyu.replay.order.producer.impl.*;
import com.jiuyu.replay.order.vo.CommodityPriceInfoVo;
import com.jiuyu.replay.order.vo.CommodityTypeListVo;
import com.jiuyu.replay.order.vo.PackageInfoVo;
import com.jiuyu.replay.order.vo.TypeConsumptionVo;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 创建套餐订单的实现类
 */
public class CreateOrderPackage extends CreateOrderBeanAbstract {

    public OrderProducer orderProducer;
    public PackageProducer packageProducer;
    public CommodityTypeProducer commodityTypeProducer;
    public CommodityPriceProducer commodityPriceProducer;
    public TypeConsumptionProducer typeConsumptionProducer;
    public OrderFeign orderFeign;

    public CreateOrderPackage() {
        orderProducer = ApplicationContextUtil.getBean(OrderProducerImpl.class);
        packageProducer = ApplicationContextUtil.getBean(PackageProducerImpl.class);
        commodityTypeProducer = ApplicationContextUtil.getBean(CommodityTypeProducerImpl.class);
        commodityPriceProducer = ApplicationContextUtil.getBean(CommodityPriceProducerImpl.class);
        typeConsumptionProducer = ApplicationContextUtil.getBean(TypeConsumptionProducerImpl.class);
        orderFeign = ApplicationContextUtil.getBean(OrderFeign.class);
    }

    @Override
    public OrderBo saveOrder(CreateOrderBo createOrderBo) {
        if (ObjectUtil.isEmpty(createOrderBo.getCommodityId())) RRException.create("版本id不能为空");
        if (ObjectUtil.isEmpty(createOrderBo.getUserId())) RRException.create("用户id不能为空");
        // 1.查询套餐信息
        PackageInfoVo packageVO = packageProducer.info(createOrderBo.getCommodityId());
        // 1.1 查询套餐类型
        List<TypeConsumptionVo> typeConsumptionVos = typeConsumptionProducer.listBySourceId(packageVO.getId(), 1);
        // 1.2 查询套餐价格
        CommodityPriceInfoVo priceVo;
        if (ObjectUtil.isEmpty(createOrderBo.getCommodityPriceId())){
            priceVo = createOrderBo.getPriceVo();
            if (ObjectUtil.isNotEmpty(priceVo.getOriginalPrice())){
                priceVo.setOriginalPrice(0);
            }
            if (ObjectUtil.isNotEmpty(priceVo.getDiscount())){
                priceVo.setDiscount(BigDecimal.valueOf(1));
            }
            if (ObjectUtil.isNotEmpty(priceVo.getRealPrice())){
                priceVo.setRealPrice(0);
            }
            if (ObjectUtil.isEmpty(priceVo.getValidityNum())) RRException.create("有效期值不能为空");
            if (ObjectUtil.isEmpty(priceVo.getValidityUnit())) RRException.create("有效期单位不能为空");
        }else {
            priceVo = commodityPriceProducer.info(createOrderBo.getCommodityPriceId());
        }
        if (ObjectUtil.isEmpty(priceVo)) RRException.create("版本价格不存在");

        Map<Long, CommodityTypeListVo> commodityTypeMap = commodityTypeProducer.list(new QueryWrapper<>())
                .stream()
                .collect(Collectors.toMap(CommodityTypeListVo::getId, Function.identity(), (existing, replacement) -> existing));


        // 2.封装订单信息
        // 创建一个新的订单业务对象
        OrderBo result = new OrderBo();
        // 设置订单ID为Snowflake算法生成的下一个值
        result.setId(SnowflakeManager.nextValue());
        // 设置用户ID为传入的创建订单业务对象中的用户ID
        result.setUserId(createOrderBo.getUserId());
        // 设置用户名为传入的创建订单业务对象中的用户名
        result.setUserName(createOrderBo.getUserName());
        // 设置商品ID为传入的套餐VO对象中的ID
        result.setCommodityId(packageVO.getId());
        // 设置商品名称为传入的套餐VO对象中的名称
        result.setCommodityName(packageVO.getName());
        // 设置订单标题为套餐VO对象中的名称
        result.setTitle(packageVO.getName());
        // 设置订单状态为0，表示未支付
        result.setStatus(0);
        // 设置订单类型为传入的创建订单业务对象中的订单类型
        result.setOrderType(createOrderBo.getOrderType());
        // 设置商品类型为1，表示套餐
        result.setCommodityType(createOrderBo.getCommodityType());
        // 设置等级为套餐对象中的等级
        result.setLevel(packageVO.getLevel());
        // 设置购买数量为1
        result.setQuantity(1);
        // 设置原价为传入的价格VO对象中的原价
        result.setOriginalPrice(priceVo.getOriginalPrice());
        // 设置折扣价为传入的价格VO对象中的折扣价
        result.setDiscount(priceVo.getDiscount());
        // 设置折扣率为传入的创建订单业务对象中的折扣率
        result.setDiscountRate(createOrderBo.getDiscountRate());
        // 计算订单价格 = 折扣价 - 优惠价格
        result.setRealPrice(priceVo.getRealPrice());
        // 计算总价 = 实际价格 * 数量
        result.setTotalPrice((priceVo.getRealPrice() - createOrderBo.getDiscountRate()) * result.getQuantity());
        if (result.getTotalPrice() < 0){
            RRException.create("订单价格不能为负数");
        }
        // 设置有效期为传入的价格VO对象中的有效期数值
        result.setExpiration(priceVo.getValidityNum());
        // 设置有效期单位为传入的价格VO对象中的有效期单位
        result.setExpirationUnit(priceVo.getValidityUnit());
        // 设置支付日期为空
        result.setPayDate(null);
        // 设置结束日期为空
        result.setEndDate(null);
        // 设置升级前信息为传入的创建订单业务对象中的升级前信息
        result.setBeforeUpgrading(createOrderBo.getBeforeUpgrading());
        // 设置升级后信息为空
        result.setAfterUpgrading(null);
        // 设置来源为传入的创建订单业务对象中的来源
        result.setSource(createOrderBo.getSource());
        // 设置创建日期为当前日期
        Date date = new Date();
        result.setCreateDate(date);
        // 设置更新日期为当前日期
        result.setUpdateDate(date);
        // 设置是否删除为0，表示未删除
        result.setIsDeleted(0);
        // 是否是试用订单
        result.setTrialOrder(priceVo.getTrialVersion());

        // 2.1 封装订单详情
        ArrayList<OrderDetailBo> orderDetailList = new ArrayList<>();
        // 如果消费类型VO列表不为空，则遍历列表封装每个消费类型的订单详情
        if (typeConsumptionVos != null) {
            for (TypeConsumptionVo item : typeConsumptionVos) {
                OrderDetailBo orderDetailBo = new OrderDetailBo();
                // 设置订单详情ID为Snowflake算法生成的下一个值
                orderDetailBo.setId(SnowflakeManager.nextValue());
                // 设置订单ID为刚刚创建的订单ID
                orderDetailBo.setOrderId(result.getId());
                // 设置商品ID为当前消费类型的ID
                orderDetailBo.setCommodityId(item.getCommodityTypeId());
                // 设置商品名称为当前消费类型的名称
                orderDetailBo.setCommodityName(item.getCommodityTypeName());
                // 设置商品类型ID为当前消费类型的ID
                orderDetailBo.setCommodityTypeId(item.getCommodityTypeId());
                // 设置商品类型代码为当前消费类型的代码
                orderDetailBo.setCommodityTypeCode(item.getCommodityTypeCode());
                // 设置商品类型名称为当前消费类型的名称
                orderDetailBo.setCommodityTypeName(item.getCommodityTypeName());
                // 设置商品类型单位为当前消费类型的单位
                orderDetailBo.setCommodityTypeUnit(item.getCommodityTypeUnit());
                // 设置商品类型是否重置为当前消费类型的重置状态
                orderDetailBo.setCommodityTypeReset(item.getCommodityTypeReset());
                // 设置总数量为当前消费类型的数量
                orderDetailBo.setTotalNumber(item.getNumber());
                // 设置开始使用时间为空
                orderDetailBo.setStartDate(null);
                CommodityTypeListVo commodityTypeVo = commodityTypeMap.get(item.getCommodityTypeId());
                RRException.isNotEmpty(commodityTypeVo, "商品类型不存在");
                // 设置重置数量为重置数量
                orderDetailBo.setResetNum(commodityTypeVo.getResetNum());
                // 设置重置单位为重置单位
                orderDetailBo.setResetUnit(commodityTypeVo.getResetUnit());
                // 设置下次重置日期为空
                orderDetailBo.setNextReset(null);
                // 设置到期日期为空
                orderDetailBo.setExpirationDate(null);
                // 设置状态为0未开始
                orderDetailBo.setStatus(0);
                // 设置创建日期为当前日期
                orderDetailBo.setCreateDate(date);
                // 设置更新日期为当前日期
                orderDetailBo.setUpdateDate(date);
                // 设置是否删除为0，表示未删除
                orderDetailBo.setIsDeleted(0);
                // 将当前订单详情添加到订单详情列表中
                orderDetailList.add(orderDetailBo);
            }
        }
        // 设置订单业务对象的订单详情列表
        result.setOrderDetailList(orderDetailList);
        // 返回创建好的订单业务对象
        return result;
    }

    @Override
    public boolean isUseByOrder(OrderInfoVo order) {
        if (order == null) return false;
        if (order.getStatus() != 1){
            return  false;
        }
        // 当前时间
        DateTime now = DateTime.now();
        // 定义两个时间范围
        DateTime startTime = new DateTime(order.getStartDate());
        DateTime endTime = new DateTime(order.getEndDate());
        // 检查当前时间是否在范围内
        return (now.toString("yyyy-MM-dd HH:mm:ss").compareTo(startTime.toString("yyyy-MM-dd HH:mm:ss")) >= 0)
                && (now.toString("yyyy-MM-dd HH:mm:ss").compareTo(endTime.toString("yyyy-MM-dd HH:mm:ss")) <= 0);
    }

    @Override
    public void checkOrder(CreateOrderBo createOrderBo) {
        super.checkOrder(createOrderBo);
        if (ObjectUtil.isEmpty(createOrderBo.getCommodityId())) RRException.create("商品不能为空");
        if (ObjectUtil.isEmpty(createOrderBo.getCommodityPriceId()) && ObjectUtil.isEmpty(createOrderBo.getPriceVo())) RRException.create("商品价格不能为空");


        // 查询套餐的等级是否正常
        PackageInfoVo info = packageProducer.info(createOrderBo.getCommodityId());
        if (info == null) RRException.create("商品不存在");
        OrderInfoVo currentOrder = orderFeign.currentOrderByUserId(createOrderBo.getUserId());
        if (createOrderBo.getOrderType() == 1 || createOrderBo.getOrderType() == 2) {
            if (currentOrder != null && currentOrder.getLevel() != null && currentOrder.getLevel() > info.getLevel()){
                RRException.create("版本升级不能向下升级");
            }
            if (currentOrder != null && Objects.equals(currentOrder.getLevel(), info.getLevel())) {
                RRException.create("版本等级相同，请走续费流程, userId: " + createOrderBo.getUserId());
            }
        } else if (createOrderBo.getOrderType() == 3) {
            if (currentOrder != null && !ObjectUtil.equals(currentOrder.getLevel(), info.getLevel())) {
                //判断是否有这个版本的冻结订单
                List<OrderInfoVo> orderInfoVos = orderProducer.listFrozenOrderByUserIdAndLevel(createOrderBo.getUserId(), info.getLevel());
                if (ObjectUtil.isEmpty(orderInfoVos)) {
                    RRException.create("续费的版本不同，不能续费");
                }
            }
        } else if (createOrderBo.getOrderType() == 0) {
            // 检查当前用户是否还有套餐
            if (currentOrder != null && Objects.equals(currentOrder.getLevel(), info.getLevel())){
                RRException.create("当前版本等级一致，不能重复购买, userId: " + createOrderBo.getUserId() + " orderID：" + currentOrder.getId() + ", Level: " + currentOrder.getLevel());
            }
            if (currentOrder != null && info.getLevel() == 0 && 0 > currentOrder.getLevel()){
                createOrderBo.setBeforeUpgrading(currentOrder.getId());
            }else{
                OrderInfoVo userPackage = orderProducer.currentOrderByUserId(createOrderBo.getUserId());
                if (userPackage != null && userPackage.getLevel() > 0) {
                    RRException.create("当前用户的版本还没有到期不能在购买此版本");
                }
            }
        }else if (createOrderBo.getOrderType() == 5){

            // 查看当前还有没有版本订单
            if (createOrderBo.getBeforeUpgrading() == null){
                if (currentOrder != null) {
                    createOrderBo.setBeforeUpgrading(currentOrder.getId());
                }

            }
        }else if (createOrderBo.getOrderType() == 6){

        }
    }
}
