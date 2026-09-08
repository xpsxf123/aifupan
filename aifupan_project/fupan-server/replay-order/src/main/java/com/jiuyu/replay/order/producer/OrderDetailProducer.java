package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.order.OrderDetailInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderDetailListVo;
import com.jiuyu.replay.order.bo.OrderDetailBo;
import com.jiuyu.replay.order.bo.OrderDetailListBo;
import com.jiuyu.replay.order.vo.ClintGetDataVo;

import java.util.List;


/**
 * 
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
public interface OrderDetailProducer {


    /**
     * 列表
     * @param orderDetailListBo 列表查询参数
     * @return
     */
    PageUtils<OrderDetailListVo> queryPage(OrderDetailListBo orderDetailListBo);

    /**
    * 信息
    * @param id id
    * @return
    */
    OrderDetailInfoVo info(Long id);

    /**
     * 新增
     * @param orderDetailBo 对象
     * @return
     */
     OrderDetailInfoVo save(OrderDetailBo orderDetailBo);

    /**
     * 修改
     * @param orderDetailBo 对象
     * @return
     */
    void update(OrderDetailBo orderDetailBo);

    /**
     * 删除
     * @param id id
     * @return
     */
    void deleteById(Long id);

    /**
     * 批量新增
     * @param orderDetailList
     */
    void saveBatch(List<OrderDetailBo> orderDetailList);

    /**
     * 更新订单状态
     * @param orderId
     * @param status
     */
    void updateStatus(Long orderId, int status);

    /**
     * 根据订单id获取订单详情
     * @param orderId
     * @return
     */
    List<OrderDetailInfoVo> listByOrderId(Long orderId);


    List<ClintGetDataVo> getDetailByOrderIds(List<Long> orderIds);


    /**
     * 批量更新订单状态
     * @param orderIds 订单id
     * @param status 状态
     */
    void batchUpdateStatusInOrderId(List<Long> orderIds, int status);
}

