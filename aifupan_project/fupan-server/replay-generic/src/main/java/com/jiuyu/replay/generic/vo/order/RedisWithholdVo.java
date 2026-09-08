package com.jiuyu.replay.generic.vo.order;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/23 上午10:11
 */
@Data
public class RedisWithholdVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * redis缓存id
     */
    private Long redisId;

    /**
     * 预扣id
     */
    private String withholdId;

    /**
     * 数量
     */
    private Long num;

    /**
     * 资产id
     */
    private Long propertyId;

    /**
     * 资产类型
     */
    private String commodityTypeCode;

    /**
     * 过期时间
     */
    private Long expirationTime;
}
