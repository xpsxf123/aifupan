package com.jiuyu.replay.power.bll;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.power.bo.UserDeviceFingerprintBo;
import com.jiuyu.replay.power.entity.UserDeviceFingerprintEntity;
import com.jiuyu.replay.power.repository.service.UserDeviceFingerprintService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author RayChou
 * @date 2025/6/10 18:27
 */
@Component
@AllArgsConstructor
public class UserDeviceFingerprintBll {

    private final UserDeviceFingerprintService userDeviceFingerprintService;

    public UserDeviceFingerprintBo getUserDeviceFingerprintByFingerprintAndDeviceType(String fingerprint, Integer deviceType) {
        UserDeviceFingerprintEntity userDeviceFingerprintEntity = userDeviceFingerprintService.getOne(new LambdaQueryWrapper<>(UserDeviceFingerprintEntity.class).eq(UserDeviceFingerprintEntity::getFingerprint, fingerprint).eq(UserDeviceFingerprintEntity::getDeviceType, deviceType));
        return BeanConvertUtils.convert(userDeviceFingerprintEntity, UserDeviceFingerprintBo.class);
    }

    public UserDeviceFingerprintBo getUserDeviceFingerprintByFingerprint(String fingerprint) {
        UserDeviceFingerprintEntity userDeviceFingerprintEntity = userDeviceFingerprintService.getOne(new LambdaQueryWrapper<>(UserDeviceFingerprintEntity.class).eq(UserDeviceFingerprintEntity::getFingerprint, fingerprint));
        return BeanConvertUtils.convert(userDeviceFingerprintEntity, UserDeviceFingerprintBo.class);
    }

    /**
     * 保存设备指纹信息
     *
     * @param userDeviceFingerprintBo
     * @return
     */
    public boolean saveUserDeviceFingerprint(UserDeviceFingerprintBo userDeviceFingerprintBo) {
        UserDeviceFingerprintEntity userDeviceFingerprintEntity = BeanConvertUtils.convert(userDeviceFingerprintBo, UserDeviceFingerprintEntity.class);
        if (Objects.isNull(userDeviceFingerprintEntity)) {
            return false;
        }
        return userDeviceFingerprintService.save(userDeviceFingerprintEntity);
    }
}
