package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/23 18:42
 */
@Data
@Schema(description = "订单最终返回")
public class OrderFinallyVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;
    /**
     * 用户名称
     */
    private String userName;
    /**
     * 商品id
     */
    private Long commodityId;
    /**
     * 商品名称
     */
    private String commodityName;
    /**
     * 升级等级
     */
    private Integer level;
    /**
     * 订单生效时间
     */
    private Date startDate;
    /**
     * 订单过期时间
     */
    private Date endDate;
    /**
     * 是否是试用订单 0否，1是
     */
    private Integer trialOrder;
}
