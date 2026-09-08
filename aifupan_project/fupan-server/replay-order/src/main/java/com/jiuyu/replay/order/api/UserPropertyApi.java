package com.jiuyu.replay.order.api;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.MonitorPositionAuthVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/7/1 下午3:30
 */
@Service
@AllArgsConstructor
@Slf4j
public class UserPropertyApi implements UserPropertyFeign {

    private final UserPropertyBll userPropertyBll;
    private final UserFeign userFeign;

    @Override
    public List<UserPropertyTypeInfoVo> getUserProperty(Long userId) {
        return userPropertyBll.getUserProperty(userId);
    }

    @Override
    public boolean updateByPropertyNumRetBoolean(Long userId, String code, Long quantity) {
        return userPropertyBll.updateByPropertyNumRetBoolean(userId, code, quantity);
    }

    @Override
    public boolean checkUseProperty(Long userId, String code, long quantity) {
        // 检查是否是订阅资产
        boolean isSubscribe = OrderEnums.commodityTypeCode.isUserShortVideoProperty(code);
        if (isSubscribe) {
            return userPropertyBll.checkSubscribeUseProperty(userId, code, quantity);
        }
        return userPropertyBll.checkUseProperty(userId, code, quantity);
    }

    @Override
    public boolean useShortVideoProperty(Long userId, String code, Long quantity) {
        if (quantity == 0) {
            return false;
        }
        OrderEnums.commodityTypeCode userSubscribeEnum = OrderEnums.commodityTypeCode.getUserSubscribeEnum(code);
        if (userSubscribeEnum == null) {
            quantity = -Math.abs(quantity);
            return useProperty(userId, code, quantity);
        } else {
            return useSubscribeProperty(userId, code, quantity);
        }
    }

    @Override
    public MonitorPositionAuthVo checkMonitorPosition(Long userId, String baseCode) {
        return userPropertyBll.checkMonitorPosition(userId, baseCode);
    }

    @Override
    public void syncSubAccountCount(Long userId) {
        UserDto user = userFeign.userById(userId);
        if (user == null) {
            return;
        }
        // 判断是否是子账号
        if (ObjectUtil.equals(user.getUserType(), UserEnums.userType.CLIENT_CHILD_USER.getCode())) {
            user = userFeign.userById(user.getParentId());
        }

        // 是主账号后，就重新统计子账号的数量
        if (ObjectUtil.equals(user.getUserType(), UserEnums.userType.CLIENT_USER.getCode())) {
            Long count = userFeign.getSubUserCountByUserId(userId);
            updateByPropertyNumRetBoolean(userId, "subAccountCount", count);
        }
    }

    /**
     * 使用资产--短视频订阅
     *
     * @param userId   用户id
     * @param code     code
     * @param quantity 使用量
     * @return 是否成功
     */
    private boolean useSubscribeProperty(Long userId, String code, Long quantity) {
        UserDto userDto = userFeign.userById(userId);
        if (userDto == null) {
            return false;
        }
        return userPropertyBll.useSubscribeProperty(userId, userDto.getActiveTenantId(), code, quantity);
    }

    /**
     * 使用资产
     *
     * @param userId   用户id
     * @param code     code
     * @param quantity 使用量
     * @return 是否成功
     */
    private boolean useProperty(Long userId, String code, Long quantity) {
        UserDto userDto = userFeign.userById(userId);
        if (userDto == null) {
            return false;
        }
        AssetsMinusOrPlusBo assets = new AssetsMinusOrPlusBo();
        assets.setUserId(userId);
        assets.setUserName(userDto.getNickName());
        assets.setNum(quantity);
        assets.setCode(code);
        boolean result = false;
        try {
            userPropertyBll.useProperty(assets);
            result = true;
        } catch (RRException | BusinessException e) {
            log.warn(e.getMessage());
        } catch (Exception e) {
            log.error("使用资产报错：" + e.getMessage(), e);
        }
        return result;
    }
}
