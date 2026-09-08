package com.jiuyu.replay.generic.feign.power;

import com.jiuyu.replay.generic.dto.power.UserDeviceFingerprintDto;

/**
 * @author RayChou
 * @date 2025/6/10 18:22
 */
public interface UserDeviceFingerprintFeign {

    /**
     * 查询指纹设备信息根据指纹和设备类型
     *
     * @param fingerprint
     * @param deviceType
     * @return
     */
    UserDeviceFingerprintDto getUserDeviceFingerprintByFingerprintAndDeviceType(String fingerprint, Integer deviceType);

    /**
     * 查询指纹设备信息根据指纹
     *
     * @param fingerprint
     * @return
     */
    UserDeviceFingerprintDto getUserDeviceFingerprintByFingerprint(String fingerprint);

    /**
     * 保存指纹设备信息
     *
     * @param userDeviceFingerprintDto
     * @return
     */
    boolean saveUserDeviceFingerprint(UserDeviceFingerprintDto userDeviceFingerprintDto);
}
