package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/24 17:31
 */
@Data
@Schema(description = "看板数据的统计回参")
public class DashboardListVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private Long id;

    @Schema(description = "微信名称")
    private String wxName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "渠道id")
    private Long channelId;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "客户意向")
    private String userAmbition;

    @Schema(description = "销售id")
    private Long salesId;

    @Schema(description = "销售名称")
    private String salesName;

    @Schema(description = "到期时间(小时)")
    private Integer expireDays;

    @Schema(description = "用户的订单状态 0：在用，1：已过期")
    private Integer orderStatus;

    @Schema(description = "到期时间")
    private Date expirationDate;

    @Schema(description = "等级")
    private Integer level;

    @Schema(description = "等级")
    private Integer levelTwo;

    @Schema(description = "未过期时间")
    private Date expirationOneDate;

    @Schema(description = "已过期时间")
    private Date expirationTwoDate;

    @Schema(description = "套餐名称")
    private String packageName;

    @Schema(description = "客户类型")
    private String userBelongType;
}
