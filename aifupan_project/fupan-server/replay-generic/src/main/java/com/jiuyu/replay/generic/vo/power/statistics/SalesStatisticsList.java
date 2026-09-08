package com.jiuyu.replay.generic.vo.power.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/30 11:10
 */
@Data
public class SalesStatisticsList implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "微信名称")
    private String wxName;

    @Schema(description = "电话")
    private String phone;

    @Schema(description = "注册时间")
    private String registerDate;

    @Schema(description = "版本等级")
    private String packageLevel;

    @Schema(description = "版本名称")
    private String packageName;

    @Schema(description = "客户意向度")
    private String userAmbition;

    @Schema(description = "到期剩余天数 (单位：小时)")
    private Integer packageExpires;

    @Schema(description = "版本到期时间")
    private String packageExpiresTime;

    @Schema(description = "跟进状态")
    private String accordingStatus;

    @Schema(description = "跟进状态名称")
    private String accordingStatusName;

    @Schema(description = "跟进内容")
    private String accordingContent;

    @Schema(description = "跟进时间")
    private String accordingDate;

    @Schema(description = "下次跟进时间")
    private String nextFolTime;

    @Schema(description = "销售名称")
    private String salesName;

    @Schema(description = "销售id")
    private Long salesId;
}
