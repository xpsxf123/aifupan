package com.jiuyu.replay.power.api;

import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.generic.dto.power.UserDeviceFingerprintDto;
import com.jiuyu.replay.generic.feign.power.UserDeviceFingerprintFeign;
import com.jiuyu.replay.power.bll.UserDeviceFingerprintBll;
import com.jiuyu.replay.power.bo.UserDeviceFingerprintBo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author RayChou
 * @date 2025/6/10 18:26
 */
@Component
@AllArgsConstructor
public class UserDeviceFingerprintApi implements UserDeviceFingerprintFeign {

    private final UserDeviceFingerprintBll userDeviceFingerprintBll;

    @Override
    public UserDeviceFingerprintDto getUserDeviceFingerprintByFingerprintAndDeviceType(String fingerprint, Integer deviceType) {
        return BeanConvertUtils.convert(userDeviceFingerprintBll.getUserDeviceFingerprintByFingerprintAndDeviceType(fingerprint, deviceType), UserDeviceFingerprintDto.class);
    }

    @Override
    public UserDeviceFingerprintDto getUserDeviceFingerprintByFingerprint(String fingerprint) {
        return BeanConvertUtils.convert(userDeviceFingerprintBll.getUserDeviceFingerprintByFingerprint(fingerprint), UserDeviceFingerprintDto.class);
    }

    @Override
    public boolean saveUserDeviceFingerprint(UserDeviceFingerprintDto userDeviceFingerprintDto) {
        return userDeviceFingerprintBll.saveUserDeviceFingerprint(BeanConvertUtils.convert(userDeviceFingerprintDto, UserDeviceFingerprintBo.class));
    }
}
