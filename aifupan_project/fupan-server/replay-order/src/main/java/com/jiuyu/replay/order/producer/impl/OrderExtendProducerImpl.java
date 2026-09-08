package com.jiuyu.replay.order.producer.impl;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.order.OrderExtendInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderExtendListVo;
import com.jiuyu.replay.order.bo.OrderExtendBo;
import com.jiuyu.replay.order.bo.OrderExtendListBo;
import com.jiuyu.replay.order.entity.OrderExtendEntity;
import com.jiuyu.replay.order.producer.OrderExtendProducer;
import com.jiuyu.replay.order.repository.service.OrderExtendService;
import com.jiuyu.replay.order.repository.service.OrderService;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 订单的扩展表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
@Service
public class OrderExtendProducerImpl implements OrderExtendProducer {

    @Resource
    private OrderExtendService orderExtendService;
    @Resource
    private OrderService orderService;
    @Resource
    private RedissonClient redissonClient;


    /**
     * 订单的扩展表列表
     * @param orderExtendListBo 订单的扩展表列表查询参数
     * @return
     */
    @Override
    public PageUtils<OrderExtendListVo> queryPage(OrderExtendListBo orderExtendListBo) {
        LambdaQueryWrapper<OrderExtendEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtil.isNotBlank(orderExtendListBo.getKeyword()),OrderExtendEntity::getRemarks,orderExtendListBo.getKeyword())
                //筛选 订单id
                .eq(ObjectUtil.isNotNull(orderExtendListBo.getOrderId()),OrderExtendEntity::getOrderId,orderExtendListBo.getOrderId());

        //执行分页查询
        IPage<OrderExtendEntity> iPage = orderExtendService.page(new Query<OrderExtendEntity>().getPage(orderExtendListBo.getPage(), orderExtendListBo.getLimit()), wrapper);
        //封装分页数据
        PageUtils<OrderExtendListVo> pageUtils = new PageUtils<>(orderExtendListBo.getPage(), orderExtendListBo.getLimit(), iPage);

        List<OrderExtendEntity> records = iPage.getRecords();
        if(records != null && !records.isEmpty()) {
            List<OrderExtendListVo> vos = records.stream().map(item -> {
                OrderExtendListVo orderExtendVo = new OrderExtendListVo();
                BeanUtils.copyProperties(item, orderExtendVo);
                return orderExtendVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public OrderExtendInfoVo info(Long id) {

        OrderExtendEntity orderExtendEntity = orderExtendService.getById(id);
        if(orderExtendEntity != null) {
            OrderExtendInfoVo orderExtendInfoVo = new OrderExtendInfoVo();
            BeanUtils.copyProperties(orderExtendEntity, orderExtendInfoVo);
            return orderExtendInfoVo;
        }

        return null;
    }

    @Override
    public OrderExtendInfoVo save(OrderExtendBo orderExtendBo) {

         OrderExtendEntity orderExtendEntity = new OrderExtendEntity();
         BeanUtils.copyProperties(orderExtendBo, orderExtendEntity);
         orderExtendEntity.setId(SnowflakeManager.nextValue());
         orderExtendEntity.setCreateDate(new Date());
         orderExtendEntity.setUpdateDate(new Date());

         orderExtendService.save(orderExtendEntity);

         OrderExtendInfoVo orderExtendInfoVo = new OrderExtendInfoVo();
         BeanUtils.copyProperties(orderExtendEntity, orderExtendInfoVo);

         return orderExtendInfoVo;
     }

    @Override
    public void update(OrderExtendBo orderExtendBo) {

        OrderExtendEntity orderExtendEntity = new OrderExtendEntity();
        BeanUtils.copyProperties(orderExtendBo, orderExtendEntity);
        orderExtendEntity.setUpdateDate(new Date());

        orderExtendService.updateById(orderExtendEntity);
    }

    @Override
    public void deleteById(Long id) {

        orderExtendService.removeById(id);
    }

    @Override
    public OrderExtendInfoVo getByOrderId(Long orderId) {
        OrderExtendEntity orderExtend = orderExtendService.getOne(new LambdaQueryWrapper<OrderExtendEntity>()
                .eq(OrderExtendEntity::getOrderId, orderId)
                .last(" limit 1")
        );
        if (orderExtend != null) {
            return BeanConvertUtils.convert(orderExtend, OrderExtendInfoVo.class);
        }
        return null;
    }
}

