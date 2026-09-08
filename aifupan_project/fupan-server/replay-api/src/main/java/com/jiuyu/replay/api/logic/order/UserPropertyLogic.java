package com.jiuyu.replay.api.logic.order;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.MonitorPositionAuthVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.order.bo.*;
import com.jiuyu.replay.order.dto.UserPropertyTypeCacheDto;
import com.jiuyu.replay.order.vo.*;

import java.util.List;


/**
 * 用户资产
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
public interface UserPropertyLogic {


    /**
     * 用户资产列表
     * @param userPropertyListBo 用户资产列表查询参数
     * @return
     */
    R<PageUtils<UserPropertyListVo>> queryPage(UserPropertyListBo userPropertyListBo);

    /**
    * 用户资产信息
    * @param userId 用户资产id
    * @return
    */
    R<List<UserPropertyTypeInfoVo>> getPropertyByUserId(Long userId);

    /**
     * 查询用户资产信息
     * @param propertyId
     * @return
     */
    R<List<UserPropertyTypeInfoVo>> getPropertyByPropertyId(Long propertyId);

    /**
     * 新增用户资产
     * @param userPropertyBo 用户资产对象
     * @return
     */
    R<String> save(UserPropertyBo userPropertyBo);

    /**
     * 修改用户资产
     * @param userPropertyBo 用户资产对象
     * @return
     */
    R<String> update(UserPropertyBo userPropertyBo);

    /**
     * 删除用户资产
     * @param id 用户资产id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 检查用户资产，不存在则创建
     */
    void checkUserPropertyAndCreate();

    /**
     * 查询用户的资产
     * @return
     */
    R<UserPropertyTypeCacheDto> getUserProperty(Long userId);

    /**
     * 减少或增加用户资产
     * @param assets
     * @return
     */
    R<String> assetsMinusOrPlus(AssetsMinusOrPlusBo assets);

    /**
     * 减少或增加用户资产
     * @param assets
     * @return
     */
    R<String> assetsMinusOrPlusReal(AssetsMinusOrPlusBo assets);

    /**
     * 检查用户是否可以使用-aiToken
     * @param bo
     * @return
     */
    R<IsPropertyHaveVo> isPropertyHaveAiToken(IsPropertyHaveBo bo);

    /**
     * 检查用户是否可以使用资产
     *
     * @param bo@return
     */
    R<IsPropertyHaveVo> isHave(IsPropertyHaveBo bo);

    /**
     * 用户资产使用详情列表
     * @param bo
     * @return
     */
    R<PageUtils<UserPropertyDetailsListVo>> pagePropertyDetails(UserPropertyDetailsListBo bo);

    /**
     * 统计用户资产
     * @param userIds
     * @return
     */
    R<String> statisticsUserProperty(List<Long> userIds);

    /**
     * 删除临时用户资产
     *
     * @param WithholdId
     * @param redisId
     * @return
     */
    R<String> removeTempUserProperty(String WithholdId, Long redisId);

    /**
     * 刷新用户资产
     * @return
     */
    R<String> refreshProperty();

    /**
     * 查询aiToken记录列表
     *
     * @param propertyDetailsId
     * @return
     */
    R<List<AiTokenUseRecordInfoVo>> aiTokenUseRecordByDetailId(Long propertyDetailsId);

    /**
     * 客户端获取用户资产详情
     * @return
     */
    R<ClintPackageAssetsVo> clintGetData();

    /**
     * 客户端获取用户资产详情-格式化
     *
     * @param clintPackageAssetsVoR
     * @return
     */
    R<ClintPackageAssetsVo> clintGetDataFormat(R<ClintPackageAssetsVo> clintPackageAssetsVoR);

    /**
     * 三类 AI 监控能力监控位资产统计
     *
     * <p>按固定顺序（话术质检 → 话术还原度 → 互动巡检）查询当前用户的监控位授权量、使用量及剩余量。</p>
     *
     * @return 三类监控位授权量信息列表
     */
    R<java.util.List<MonitorPositionAuthVo>> monitorPositionStatistics();
}

