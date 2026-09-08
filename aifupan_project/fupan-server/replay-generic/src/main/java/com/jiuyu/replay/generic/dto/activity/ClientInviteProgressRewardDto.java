package com.jiuyu.replay.generic.dto.activity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author RayChou
 * @date 2025/6/4 16:51
 */
@Data
public class ClientInviteProgressRewardDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;
    /**
     * 进度id
     */
    private Long progressId;
    /**
     * 奖励类型 0：版本 1：增量包
     */
    private Integer rewardType;
    /**
     * 版本id（当奖励类型为版本时有）
     */
    private Long packageId;
    /**
     * 版本价格id（当奖励类型为版本时有）
     */
    private Long packagePriceId;
    /**
     * 商品类型id
     */
    private Long commodityTypeId;
    /**
     * 商品数据量
     */
    private Long commodityNumber;
    /**
     * 商品有效期值，跟validity_unit结合使用，如：3年
     */
    private Integer validityNum;
    /**
     * 商品有效期单位，0：小时 1：天 2：月 3：季度 4：半年，5：年
     */
    private Integer validityUnit;
}
