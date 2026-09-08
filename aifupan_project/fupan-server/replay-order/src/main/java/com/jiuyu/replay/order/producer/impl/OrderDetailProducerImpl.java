package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.utils.ExpirationUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.order.OrderDetailInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderDetailListVo;
import com.jiuyu.replay.order.bo.OrderDetailBo;
import com.jiuyu.replay.order.bo.OrderDetailListBo;
import com.jiuyu.replay.order.entity.*;
import com.jiuyu.replay.order.producer.OrderDetailProducer;
import com.jiuyu.replay.order.repository.service.*;
import com.jiuyu.replay.order.repository.service.impl.TypeSurplusServiceImpl;
import com.jiuyu.replay.order.vo.ClintGetDataVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Service
@AllArgsConstructor
@Slf4j
public class OrderDetailProducerImpl implements OrderDetailProducer {

    private final OrderDetailService orderDetailService;
    private final TypeSurplusServiceImpl typeSurplusService;
    private final UserPropertyDetailsService userPropertyDetailsService;
    private final UserPropertyService userPropertyService;
    private final OrderService orderService;
    private final CommodityTypeService commodityTypeService;


    @Override
    public PageUtils<OrderDetailListVo> queryPage(OrderDetailListBo orderDetailListBo) {
        QueryWrapper<OrderDetailEntity> wrapper = new QueryWrapper<OrderDetailEntity>();
        wrapper
                .lambda()
                .eq(ObjectUtil.isNotEmpty(orderDetailListBo.getOrderId()), OrderDetailEntity::getOrderId, orderDetailListBo.getOrderId())

                ;
        IPage<OrderDetailEntity> iPage = orderDetailService.page(new Query<OrderDetailEntity>().getPage(orderDetailListBo.getPage(), orderDetailListBo.getLimit()), wrapper);

        PageUtils<OrderDetailListVo> pageUtils = new PageUtils<>(orderDetailListBo.getPage(), orderDetailListBo.getLimit(), iPage);

        List<OrderDetailEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            List<OrderDetailListVo> vos = records.stream().map(item -> {
                OrderDetailListVo orderDetailVo = new OrderDetailListVo();
                BeanUtils.copyProperties(item, orderDetailVo);
                return orderDetailVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public OrderDetailInfoVo info(Long id) {

        OrderDetailEntity orderDetailEntity = orderDetailService.getById(id);
        if (orderDetailEntity != null) {
            OrderDetailInfoVo orderDetailInfoVo = new OrderDetailInfoVo();
            BeanUtils.copyProperties(orderDetailEntity, orderDetailInfoVo);
            return orderDetailInfoVo;
        }

        return null;
    }

    /**
     * 新增
     *
     * @param orderDetailBo 对象
     * @return
     */
    public OrderDetailInfoVo save(OrderDetailBo orderDetailBo) {

        OrderDetailEntity orderDetailEntity = new OrderDetailEntity();
        BeanUtils.copyProperties(orderDetailBo, orderDetailEntity);
        orderDetailEntity.setId(SnowflakeManager.nextValue());
        orderDetailEntity.setCreateDate(new Date());
        orderDetailEntity.setUpdateDate(new Date());

        orderDetailService.save(orderDetailEntity);

        OrderDetailInfoVo orderDetailInfoVo = new OrderDetailInfoVo();
        BeanUtils.copyProperties(orderDetailEntity, orderDetailInfoVo);

        return orderDetailInfoVo;
    }

    /**
     * 修改
     *
     * @param orderDetailBo 对象
     * @return
     */
    public void update(OrderDetailBo orderDetailBo) {

        OrderDetailEntity orderDetailEntity = new OrderDetailEntity();
        BeanUtils.copyProperties(orderDetailBo, orderDetailEntity);
        orderDetailEntity.setUpdateDate(new Date());

        orderDetailService.updateById(orderDetailEntity);
    }

    /**
     * 删除
     *
     * @param id id
     * @return
     */
    public void deleteById(Long id) {

        orderDetailService.removeById(id);
    }

    @Override
    public void saveBatch(List<OrderDetailBo> orderDetailList) {
        if (ObjectUtil.isNotEmpty(orderDetailList)) {
            orderDetailService.saveBatch(BeanUtil.copyToList(orderDetailList, OrderDetailEntity.class));
        }
    }

    @Override
    public void updateStatus(Long orderId, int status) {
        orderDetailService.update(new LambdaUpdateWrapper<OrderDetailEntity>()
                .set(OrderDetailEntity::getStatus, status)
                .eq(OrderDetailEntity::getOrderId, orderId)
        );
    }

    @Override
    public List<OrderDetailInfoVo> listByOrderId(Long orderId) {
        List<OrderDetailEntity> orderDetailEntities = orderDetailService.list(new LambdaQueryWrapper<OrderDetailEntity>()
                .eq(OrderDetailEntity::getOrderId, orderId)
        );
        if (ObjectUtil.isNotEmpty(orderDetailEntities)){
            return BeanUtil.copyToList(orderDetailEntities, OrderDetailInfoVo.class);
        }
        return new ArrayList<>();
    }


    @Override
    public List<ClintGetDataVo> getDetailByOrderIds(List<Long> orderIds) {
        return orderDetailService.getDetailByOrderIds(orderIds);
    }


    /**
     * 批量更新订单状态
     *
     * @param orderIds 订单id
     * @param status   状态
     */
    @Override
    public void batchUpdateStatusInOrderId(List<Long> orderIds, int status) {
        if (EmptyUtil.isEmpty(orderIds)) {
            return;
        }
        orderDetailService.lambdaUpdate()
            .in(OrderDetailEntity::getOrderId, orderIds)
            .set(OrderDetailEntity::getStatus, status)
            .ne(OrderDetailEntity::getStatus, status)
            .update();
    }
}

