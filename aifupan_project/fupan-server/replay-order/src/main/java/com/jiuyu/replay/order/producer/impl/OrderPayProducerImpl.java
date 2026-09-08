package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.order.bo.OrderDetailBo;
import com.jiuyu.replay.order.bo.OrderPayBo;
import com.jiuyu.replay.order.bo.OrderPayListBo;
import com.jiuyu.replay.order.entity.OrderPayEntity;
import com.jiuyu.replay.order.producer.OrderPayProducer;
import com.jiuyu.replay.order.repository.service.OrderPayService;
import com.jiuyu.replay.generic.vo.order.OrderPayInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderPayListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 订单-支付信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Service
public class OrderPayProducerImpl implements OrderPayProducer {

    @Resource
    private OrderPayService orderPayService;


    @Override
    public PageUtils<OrderPayListVo> queryPage(OrderPayListBo orderPayListBo) {
        LambdaQueryWrapper<OrderPayEntity> wrapper = new LambdaQueryWrapper<OrderPayEntity>();

        IPage<OrderPayEntity> iPage = orderPayService.page(new Query<OrderPayEntity>().getPage(orderPayListBo.getPage(), orderPayListBo.getLimit()), wrapper);

        PageUtils<OrderPayListVo> pageUtils = new PageUtils<>(orderPayListBo.getPage(), orderPayListBo.getLimit(), iPage);

        List<OrderPayEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<OrderPayListVo> vos = records.stream().map(item -> {
                OrderPayListVo orderPayVo = new OrderPayListVo();
                BeanUtils.copyProperties(item, orderPayVo);
                return orderPayVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public OrderPayInfoVo info(Long id) {

        OrderPayEntity orderPayEntity = orderPayService.getById(id);
        if(orderPayEntity != null) {
            OrderPayInfoVo orderPayInfoVo = new OrderPayInfoVo();
            BeanUtils.copyProperties(orderPayEntity, orderPayInfoVo);
            return orderPayInfoVo;
        }

        return null;
    }

    /**
     * 新增订单-支付信息
     * @param orderPayBo 订单-支付信息对象
     * @return
     */
     public OrderPayInfoVo save(OrderPayBo orderPayBo) {

         OrderPayEntity orderPayEntity = new OrderPayEntity();
         BeanUtils.copyProperties(orderPayBo, orderPayEntity);
         if (ObjectUtil.isEmpty(orderPayBo.getId()))orderPayEntity.setId(SnowflakeManager.nextValue());
         orderPayEntity.setCreateDate(new Date());
         orderPayEntity.setUpdateDate(new Date());

         orderPayService.save(orderPayEntity);

         OrderPayInfoVo orderPayInfoVo = new OrderPayInfoVo();
         BeanUtils.copyProperties(orderPayEntity, orderPayInfoVo);

         return orderPayInfoVo;
     }

    /**
     * 修改订单-支付信息
     * @param orderPayBo 订单-支付信息对象
     * @return
     */
    public void update(OrderPayBo orderPayBo) {

        OrderPayEntity orderPayEntity = new OrderPayEntity();
        BeanUtils.copyProperties(orderPayBo, orderPayEntity);
        orderPayEntity.setUpdateDate(new Date());

        orderPayService.updateById(orderPayEntity);
    }

    /**
     * 删除订单-支付信息
     * @param id 订单-支付信息id
     * @return
     */
    public void deleteById(Long id) {

        orderPayService.removeById(id);
    }

    @Override
    public void saveBatch(List<OrderDetailBo> orderDetailList) {
        if (ObjectUtil.isNotEmpty(orderDetailList)){
            orderPayService.saveBatch(BeanUtil.copyToList(orderDetailList, OrderPayEntity.class));
        }
    }

    @Override
    public List<OrderPayInfoVo> getValidOrders() {
        List<OrderPayEntity> list = orderPayService.list(new LambdaQueryWrapper<OrderPayEntity>()
                .eq(OrderPayEntity::getPayStatus, 0)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, OrderPayInfoVo.class);
        }
        return List.of();
    }
}

