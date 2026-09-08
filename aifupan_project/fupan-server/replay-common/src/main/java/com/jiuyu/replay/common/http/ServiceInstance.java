package com.jiuyu.replay.common.http;

import lombok.Getter;
import lombok.Setter;

/**
 * 服务实例
 *
 * @author HeHui
 * @date 2025-12-03 15:39
 */
@Getter
@Setter
public class ServiceInstance {

    /**
     * 服务实例地址
     */
    private String instanceId;


    /**
     * 服务实例权重
     */
    private int weight = 1;


    @Override
    public String toString() {
        return "ServiceInstance{" +
            "instanceId='" + instanceId + '\'' +
            ", weight=" + weight +
            '}';
    }
}
