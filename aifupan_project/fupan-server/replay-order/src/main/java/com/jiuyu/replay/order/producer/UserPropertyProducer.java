package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.order.bo.UserPropertyDetailsList;
import com.jiuyu.replay.order.vo.*;
import com.jiuyu.replay.order.bo.UserPropertyBo;
import com.jiuyu.replay.order.bo.UserPropertyListBo;

import java.util.List;


/**
 * 用户资产
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
public interface UserPropertyProducer {


    /**
     * 用户资产列表
     * @param userPropertyListBo 用户资产列表查询参数
     * @return
     */
    PageUtils<UserPropertyListVo> queryPage(UserPropertyListBo userPropertyListBo);

    /**
    * 用户资产信息
    * @param id 用户资产id
    * @return
    */
    UserPropertyInfoVo info(Long id);

    /**
     * 新增用户资产
     * @param userPropertyBo 用户资产对象
     * @return
     */
     UserPropertyInfoVo save(UserPropertyBo userPropertyBo);

    /**
     * 修改用户资产
     * @param userPropertyBo 用户资产对象
     * @return
     */
    void update(UserPropertyBo userPropertyBo);

    /**
     * 删除用户资产
     * @param id 用户资产id
     * @return
     */
    void deleteByUserId(Long id);

    /**
     * 获取用户资产
     * @param userId
     * @return
     */
    UserPropertyInfoVo getOne(Long userId);

    /**
     * 获取用户资产-没有这创建
     *
     * @param userId       用户id
     * @param isUse        是否在使用
     * @param type         类型 0自己的，1子用户
     * @param parentId      父资产id
     * @param parentUserId 父用户id
     * @return
     */
    UserPropertyInfoVo getOne(Long userId, Integer isUse, Integer type, Long parentId, Long parentUserId);

    /**
     * 批量获取用户资产
     * @param userIds
     * @return
     */
    List<UserPropertyInfoVo> getUserPropertyByUserIds(List<Long> userIds);

    /**
     * 统计用户资产
     *
     * @param userProperty
     */
    void statisticsProperty(UserPropertyInfoVo userProperty);

    /**
     * 批量统计用户资产
     * <p>
     * 该方法用于批量更新用户资产的使用状态，并根据资产类型进行数量统计，同时将结果缓存到 Redis 中。
     * 主要包括以下操作：
     * 1. 更新用户资产的 isUse 字段（标记是否被使用）；
     * 2. 将用户当前使用的资产 ID 缓存至 Redis；
     * 3. 查询相关资产类型的记录并重置其使用量和总量；
     * 4. 根据是否需要重置数据分别处理资产类型数量；
     * 5. 批量更新资产类型数据；
     * 6. 将资产类型使用量和总量缓存至 Redis。
     *
     * @param userPropertyList 用户资产信息列表，不能为空。每个元素包含用户ID、资产ID等信息。
     */
    void batchStatisticsProperty(List<UserPropertyInfoVo> userPropertyList);

    /**
     * 统计子用户资产
     * @param userPropertyInfoVo
     */
    void statisticsChildProperty(UserPropertyInfoVo userPropertyInfoVo);

    /**
     * 批量统计子用户资产
     * @param userPropertyList 用户资产信息列表，不能为空。每个元素包含用户ID、资产ID等信息。
     */
    void batchStatisticsChildProperty(List<UserPropertyInfoVo> userPropertyList);

    /**
     * 批量检查用户资产并创建
     *
     * @param userIds
     * @param isUse
     */
    void checkUserPropertyAndCreate(List<Long> userIds, Integer isUse);

    /**
     * 添加用户资产缓存
     */
    void addUserPropertyCache();

    /**
     * 用户资产减少或增加
     * @param assets
     */
    void assetsMinusOrPlus(AssetsMinusOrPlusBo assets);

    /**
     * 获取用户资产
     * @param userId
     * @return
     */
    List<UserPropertyTypeInfoVo> getUserProperty(Long userId);

    /**
     * 获取用户资产，没有就创建
     * @param id
     * @param parentId
     * @return
     */
    UserPropertyInfoVo getUserPropertyOrCreate(Long id, Long parentId);

    /**
     * 使用用户资产
     * @param id
     */
    void useChildUserProperty(Long id);

    /**
     * 更新用户资产
     *
     * @param userId
     * @param code
     * @param quantity
     * @return
     */
    boolean updateUserProperty(Long userId, String code, Long quantity);

    /**
     * 更新资产
     *
     * @param userId     用户od
     * @param propertyId 资产id
     * @param code       code
     * @param quantity   更新值
     * @return 是否成功
     */
    boolean updateUserPropertyType(Long userId, Long propertyId, String code, Long quantity);

    /**
     * 查询用户资产信息
     * @param userId
     * @param code
     * @return
     */
    Long getTotalUserIdPropertyByCode(Long userId, String code);

    /**
     * 查询aiToken记录列表
     *
     * @param propertyDetailsId
     * @return
     */
    List<AiTokenUseRecordInfoVo> aiTokenUseRecordByDetailId(Long propertyDetailsId);

    /**
     * 根据用户id 获取该用户正在试用的资产id
     * @param userId
     * @return
     */
    Long clintGetData(Long userId);

    Long getCurrentUserId(Long userId, String code);

    /**
     * 清空用户资产-不是公用的资产
     * @param userIds 用户id
     * @param code 资产类型code
     */
    void clearPrivateProperty(List<Long> userIds, String code);

    /**
     * 更新共享的资产和自己的资产
     */
    void useSubscribeProperty(List<UpdateUserPropertyVo> list);

    /**
     * 用户资产详情
     *
     * @param bo 查询参数
     * @return 列表
     */
    PageUtils<UserPropertyDetailsInfoVo> userPropertyDetails(UserPropertyDetailsList bo);
}

