package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.order.OrderDetailInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.order.bo.TypeSurplusBo;
import com.jiuyu.replay.order.bo.TypeSurplusListBo;
import com.jiuyu.replay.order.vo.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 商品类型资产剩余表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
public interface TypeSurplusProducer {


    /**
     * 商品类型资产剩余表列表
     * @param typeSurplusListBo 商品类型资产剩余表列表查询参数
     * @return
     */
    PageUtils<TypeSurplusListVo> queryPage(TypeSurplusListBo typeSurplusListBo);

    /**
    * 商品类型资产剩余表信息
    * @param id 商品类型资产剩余表id
    * @return
    */
    TypeSurplusInfoVo info(java.lang.Long id);

    /**
     * 新增商品类型资产剩余表
     * @param typeSurplusBo 商品类型资产剩余表对象
     * @return
     */
     TypeSurplusInfoVo save(TypeSurplusBo typeSurplusBo);

    /**
     * 修改商品类型资产剩余表
     * @param typeSurplusBo 商品类型资产剩余表对象
     * @return
     */
    void update(TypeSurplusBo typeSurplusBo);

    /**
     * 删除商品类型资产剩余表
     * @param id 商品类型资产剩余表id
     * @return
     */
    void deleteById(java.lang.Long id);

    /**
     * 支付后处理
     * @param orderInfoVo
     * @param userPropertyInfoVo
     */
    void payAddTypeSurplus(OrderInfoVo orderInfoVo, UserPropertyInfoVo userPropertyInfoVo);

    void payAddTypeSurplus1(OrderInfoVo newOrder, UserPropertyInfoVo userPropertyInfoVo);


    /**
     * 支付后批量处理
     * @param orderPropertyMap 订单和用户资产
     */
    void batchPayAddTypeSurplus(Map<OrderInfoVo, UserPropertyInfoVo> orderPropertyMap);

    /**
     * 批量补救漏建的 type_surplus：检查应建但实际未建的主账号资产明细并补建
     * @param orderPropertyMap 订单和用户资产
     */
    void remedyMissedTypeSurplus(Map<OrderInfoVo, UserPropertyInfoVo> orderPropertyMap);

    /**
     * 添加子用户资产
     * @param orderInfoVo
     */
    void payAddChildTypeSurplus(OrderInfoVo orderInfoVo);

    /**
     * 批量添加子用户资产
     * @param orderInfoVos 订单列表
     */
    void batchPayAddChildTypeSurplus(Collection<OrderInfoVo> orderInfoVos);

    /**
     * 同步订单详情和用户资产
     *
     * @param orderDetailsList
     */
    void synchronousTypeSurplus(List<OrderDetailInfoVo> orderDetailsList);

    /**
     * 获取当前用户剩余
     * @param propertyId
     * @param commodityTypeId
     * @return
     */
    List<TypeSurplusInfoVo> currentSurplus(Long propertyId, Long commodityTypeId);

    void updateBatch(List<TypeSurplusBo> typeSurplusBos);


    /**
     * 根据详情结果，查需重置的资产的用量和总理
     * 并且根据订单详情id设置对应的资产用量，剩余，总量
     * @param details
     */
    void selectByDetailIds(List<ClintGetDataVo> details);

    /**
     * 获取当前用户剩余数量
     * @param propertyId 资产ID
     * @param commodityTypeId 套餐资产类型ID
     * @return 总剩余数量
     */
    Long currentSurplusCount(Long propertyId, Long commodityTypeId);
}

