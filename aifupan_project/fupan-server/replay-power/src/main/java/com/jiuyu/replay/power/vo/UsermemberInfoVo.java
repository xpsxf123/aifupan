package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * @author tisheng
 * @date 2024/9/6
 * @apinNote
 */
@Data
public class UsermemberInfoVo {
    @Schema(description = "昵称")
    private String nickName;
    @Schema(description = "userId")
    private Long id;
    @Schema(description = "登录账号")
    private String username;
    @Schema(description = "会员等级")
    private String ViPName;
    @Schema(description = "拥有监控位数量")
    private Integer monitorNum;
    @Schema(description = "拥有分析时长")
    private Double aiAnalysisTime;
    @Schema(description = "到期时间(订单表的过期时间)")
    private Date expirationDate;

    @Schema(description = "可添加主播数量")
    private Integer anchorNum;

    @Schema(description = "已添加主播数量")
    private Integer sum;

    @Schema(description = "手机号码")
    private String phone;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;



}
