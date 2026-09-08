package com.jiuyu.replay.generic.feign.order;

import com.jiuyu.replay.generic.vo.order.MonitorPositionAuthVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;

import java.util.List;

/**
 * &#064;ClassName:  UserPropertyFeign
 * &#064;Description:  用户资产
 *
 * @author xujiujj
 * @date 2021年12月23日
 */
public interface UserPropertyFeign {

    /**
     * 获取用户资产
     *
     * @param userId
     * @return
     */
    List<UserPropertyTypeInfoVo> getUserProperty(Long userId);

    /**
     * 更新资产
     *
     * @param userId   用户id
     * @param code     资产code
     * @param quantity 量
     * @return
     */
    boolean updateByPropertyNumRetBoolean(Long userId, String code, Long quantity);

    /**
     * 检查资产是否能能用
     *
     * @param userId   用户id
     * @param code     资产code {@link com.jiuyu.replay.common.constant.OrderEnums.commodityTypeCode} 订阅达人使用subscribeInfluencerNum，订阅爆款使用subscribeHotVideoNum
     * @param quantity 要使用数量 正的是加资产，负的是减资产
     * @return 是否可以用
     */
    boolean checkUseProperty(Long userId, String code, long quantity);

    /**
     * 使用资产--短视频的资产
     *
     * @param userId   用户id
     * @param code     资产code {@link com.jiuyu.replay.common.constant.OrderEnums.commodityTypeCode} 订阅达人数量:subscribeInfluencerNum，订阅爆款数量:subscribeHotVideoNum
     * @param quantity 使用数量 正的是加资产，负的是减资产
     * @return 是否成功
     */
    boolean useShortVideoProperty(Long userId, String code, Long quantity);

    /**
     * 同步子账号数量
     *
     * @param userId 用户id
     */
    void syncSubAccountCount(Long userId);

    /**
     * 检查监控位授权量
     *
     * <p>根据资产 code 查询当前用户（或其主账号）的监控位授权量、使用量及剩余量。
     * 三类 AI 监控能力（scriptQualityNum / scriptFidelityNum / interactionPatrolNum）的
     * sub_account_have=0，子账号调用时自动回退主账号查询，无需调用方额外处理。</p>
     *
     * @param userId   用户 id（可为主账号或子账号）
     * @param baseCode 资产 code，取自 {@link com.jiuyu.replay.common.constant.OrderEnums.commodityTypeCode}
     * @return 监控位授权量信息（hasAuth / hasSurplus / totalQuantity / useQuantity / surplus）
     */
    MonitorPositionAuthVo checkMonitorPosition(Long userId, String baseCode);
}
