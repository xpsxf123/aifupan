package com.jiuyu.replay.order.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderDetailInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderDetailListVo;
import com.jiuyu.replay.order.bo.OrderDetailBo;
import com.jiuyu.replay.order.bo.OrderDetailListBo;
import com.jiuyu.replay.order.entity.UserPropertyEntity;
import com.jiuyu.replay.order.producer.OrderDetailProducer;
import com.jiuyu.replay.order.producer.OrderProducer;
import com.jiuyu.replay.order.producer.UserPropertyProducer;
import com.jiuyu.replay.order.vo.ClintGetDataVo;
import com.jiuyu.replay.order.vo.UserPropertyInfoVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Component
@Slf4j
@AllArgsConstructor
public class OrderDetailBll {

    private final OrderDetailProducer orderDetailProducer;
    private final OrderProducer orderProducer;
    private final UserPropertyProducer userPropertyProducer;


    /**
     * 列表
     *
     * @param orderDetailListBo 列表查询参数
     *
     * @return
     */
    public R<PageUtils<OrderDetailListVo>> queryPage(OrderDetailListBo orderDetailListBo) {

        return R.ok("获取成功", orderDetailProducer.queryPage(orderDetailListBo));
    }

    /**
     * 信息
     *
     * @param id id
     *
     * @return
     */
    public R<OrderDetailInfoVo> info(Long id) {

        OrderDetailInfoVo orderDetailInfoVo = orderDetailProducer.info(id);
        return R.ok("获取成功", orderDetailInfoVo);
    }

    /**
     * 新增
     *
     * @param orderDetailBo 对象
     *
     * @return
     */
    public R<String> save(OrderDetailBo orderDetailBo) {

        OrderDetailInfoVo orderDetailInfoVo = orderDetailProducer.save(orderDetailBo);
        return R.ok("添加成功");
    }

    /**
     * 修改
     *
     * @param orderDetailBo 对象
     *
     * @return
     */
    public R<String> update(OrderDetailBo orderDetailBo) {

        orderDetailProducer.update(orderDetailBo);
        return R.ok("修改成功");
    }

    /**
     * 删除
     *
     * @param id id
     *
     * @return
     */
    public R<String> delete(Long id) {

        orderDetailProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 订单详情到月底过期处理
     *
     * @return
     */
    public R<String> expiredOrderProcessing(Long orderId) {
        log.info("开始执行订单详情到月底过期处理");
        List<OrderDetailInfoVo> monthExpiredOrders = orderProducer.getMonthExpiredOrders(orderId);
        log.info("查询到{}条订单详情需要处理", monthExpiredOrders.size());
        if (ObjectUtil.isNotEmpty(monthExpiredOrders)) {
            log.info("开始处理订单详情={}", JSONUtil.toJsonStr(monthExpiredOrders));
            log.info("开始处理订单详情,第一步修改状态停止");
            orderProducer.setExpiration(monthExpiredOrders);
            log.info("修改状态停止完成");
            log.info("开始处理订单详情,第二步计算下一次月底过期时间");
            List<UserPropertyEntity> userPropertyEntityList = orderProducer.setNextExpirationTime(monthExpiredOrders);

            if (ObjectUtil.isNotEmpty(userPropertyEntityList)) {
                log.info("查询到{}条用户资产需要处理", userPropertyEntityList.size());
                for (UserPropertyEntity userProperty : userPropertyEntityList) {
                    log.info("用户资产id={}，开始统计资产", userProperty.getId());
                    UserPropertyInfoVo properties = BeanUtil.copyProperties(userProperty, UserPropertyInfoVo.class);
                    if (properties.getIsUse() == 1) {
                        userPropertyProducer.statisticsProperty(properties);
                        userPropertyProducer.statisticsChildProperty(properties);
                    }
                }
            }
        }
        return R.ok("完成");
    }



    public void executeOrderDetailExpiredSingle(List<OrderDetailInfoVo> list, int batchNo) {
        log.info("[资产过期] batch {} expired order detail >>>>>>> step 1 update status execute", batchNo);
        // 设置订单过期状态
        orderProducer.setExpiration(list);
        log.info("[资产过期] batch {} expired order detail >>>>>>> step 1 update status success.", batchNo);

        log.info("[资产过期] batch {} expired order detail >>>>>>> step 2 calc next expires at end month execute", batchNo);
        // 设置下次过期时间
        List<UserPropertyEntity> userPropertyEntityList = orderProducer.setNextExpirationTime(list);
        if (EmptyUtil.isEmpty(userPropertyEntityList)) {
            return;
        }
        List<UserPropertyEntity> useUserPropertyEntityList = userPropertyEntityList.stream().filter(userProperty -> userProperty.getIsUse() == 1).toList();
        if (EmptyUtil.isEmpty(useUserPropertyEntityList)) {
            return;
        }
        List<UserPropertyInfoVo> userPropertyInfoList = BeanUtil.copyToList(useUserPropertyEntityList, UserPropertyInfoVo.class);
        log.info("[资产过期] batch {} expired order detail >>>>>>> step 3 use detail statistics property prepare size:{}", batchNo, useUserPropertyEntityList.size());
        userPropertyInfoList.forEach(userProperty -> {
            userPropertyProducer.statisticsProperty(userProperty);
            userPropertyProducer.statisticsChildProperty(userProperty);
        });
    }

    /**
     * 执行订单详情过期处理
     *
     * @param list 过期的订单详情列表
     * @param batchNo 批次号
     */
    @Transactional(rollbackFor = Throwable.class)
    public void executeOrderDetailExpired(List<OrderDetailInfoVo> list, int batchNo) {
        log.info("[资产过期] batch {} expired order detail >>>>>>> step 1 update status execute", batchNo);
        // 设置订单过期状态
        orderProducer.setExpiration(list);
        log.info("[资产过期] batch {} expired order detail >>>>>>> step 1 update status success.", batchNo);

        log.info("[资产过期] batch {} expired order detail >>>>>>> step 2 calc next expires at end month execute", batchNo);
        // 设置下次过期时间
        List<UserPropertyEntity> userPropertyEntityList = orderProducer.setNextExpirationTime(list);
        log.info("[资产过期] batch {} expired order detail >>>>>>> step 2 calc next expires at end month success.", batchNo);
        if (EmptyUtil.isEmpty(userPropertyEntityList)) {
            return;
        }
        List<UserPropertyEntity> useUserPropertyEntityList = userPropertyEntityList.stream().filter(userProperty -> userProperty.getIsUse() == 1).toList();
        if (EmptyUtil.isEmpty(useUserPropertyEntityList)) {
            return;
        }
        log.info("[资产过期] batch {} expired order detail >>>>>>> step 3 use detail statistics property prepare size:{}", batchNo, useUserPropertyEntityList.size());
        List<UserPropertyInfoVo> userPropertyInfoList = BeanUtil.copyToList(useUserPropertyEntityList, UserPropertyInfoVo.class);
        userPropertyProducer.batchStatisticsProperty(userPropertyInfoList);
        log.info("[资产过期] batch {} expired order detail >>>>>>> step 3 use detail statistics property success.", batchNo);
        userPropertyProducer.batchStatisticsChildProperty(userPropertyInfoList);
        log.info("[资产过期] batch {} expired order detail >>>>>>> step 3 use child detail statistics property success.", batchNo);
    }


    public R<List<OrderDetailInfoVo>> listByOrderId(Long orderId) {

        return R.ok("获取成功", orderDetailProducer.listByOrderId(orderId));
    }


    public List<ClintGetDataVo> getDetailByOrderIds(List<Long> orderIds) {
        return orderDetailProducer.getDetailByOrderIds(orderIds);
    }
}

