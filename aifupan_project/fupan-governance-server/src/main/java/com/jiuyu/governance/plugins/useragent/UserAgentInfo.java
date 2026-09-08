package com.jiuyu.governance.plugins.useragent;

import lombok.Data;

/**
 * 客户端信息
 *
 * @author HeHui
 * @date 2026-03-01 20:28
 */
@Data
public class UserAgentInfo {

    /**
     * 设备类型
     */
    private ClientDeviceType deviceType = ClientDeviceType.OTHER;

    /**
     * 品牌名称
     */
    private String deviceName = "未知";

}
