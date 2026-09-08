package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.generic.vo.order.UserPropertyTypeListVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.order.bo.UserPropertyTypeBo;
import com.jiuyu.replay.order.bo.UserPropertyTypeListBo;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 用户资产类型总明细
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
public interface UserPropertyTypeProducer {


    /**
     * 用户资产类型总明细列表
     * @param userPropertyTypeListBo 用户资产类型总明细列表查询参数
     * @return
     */
    PageUtils<UserPropertyTypeListVo> queryPage(UserPropertyTypeListBo userPropertyTypeListBo);

    /**
    * 用户资产类型总明细信息
    * @param id 用户资产类型总明细id
    * @return
    */
    UserPropertyTypeInfoVo info(Long id);

    /**
     * 新增用户资产类型总明细
     * @param userPropertyTypeBo 用户资产类型总明细对象
     * @return
     */
     UserPropertyTypeInfoVo save(UserPropertyTypeBo userPropertyTypeBo);

    /**
     * 修改用户资产类型总明细
     * @param userPropertyTypeBo 用户资产类型总明细对象
     * @return
     */
    void update(UserPropertyTypeBo userPropertyTypeBo);

    /**
     * 删除用户资产类型总明细
     * @param id 用户资产类型总明细id
     * @return
     */
    void deleteById(Long id);


    /**
     * 获取用户剩余资产-如果当前资产是子用户的，并且是公用的资源，就返回父用户的剩余资产
     * @param propertyId
     * @param commodityCode
     * @return
     */
    UserPropertyTypeInfoVo getUserSRemainingAssets(Long propertyId, String commodityCode);

    /**
     * 更新当前用户剩余资产
     *
     * @param id
     * @param currentSurplusCount 当前用户剩余资产
     */
    void updateCurrent(Long id, Long currentSurplusCount);

    /**
     * 根据资产类型id查询资产类型信息
     * @param propertyId
     * @return
     */
    List<UserPropertyTypeInfoVo> getPropertyByPropertyId(Long propertyId);

    /**
     * 根据用户id和用户资产id获取用户资产类型(parentId=0)信息
     * @param userId
     * @param propertyId
     * @return
     */
    List<UserPropertyTypeListVo> clintGetTypeData(Long userId, Long propertyId);


    /**
     * 获取用户剩余资产
     *
     * @param userIds       用户ID
     * @param commodityCode 资产类型
     *
     * @return {@link Map }<{@link Long }, {@link Long }> key 用户ID value 剩余资产
     */
    Map<Long, Long> getUserPropertyRemainingMap(Collection<Long> userIds, String commodityCode);
}

