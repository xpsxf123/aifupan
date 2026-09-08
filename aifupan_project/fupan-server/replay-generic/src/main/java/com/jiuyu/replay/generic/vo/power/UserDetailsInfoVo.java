package com.jiuyu.replay.generic.vo.power;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户详情信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:41
 */
@Data
@Schema(description = "用户详情信息")
public class UserDetailsInfoVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "行业名称")
    private Long tradeName;

    @Schema(description = "父用户id")
    private Long parentId;

    @Schema(description = "用户类型")
    private Integer userType;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "用户注册时间")
    private Date registerDate;

    @Schema(description = "冻结状态 0：未冻结 1：已冻结")
    private Integer userStatus;

    @Schema(description = "版本等级")
    private Integer packageLevel;

    @Schema(description = "版本名称")
    private String packageName;

    @Schema(description = "版本过期时间")
    private Date expirationDate;

    /**
     * 用户来源渠道ID
     */
    @Schema(description = "用户来源渠道ID")
    private Long channelId;

    /**
     * 用户来源渠道名
     */
    @Schema(description = "用户来源渠道名")
    private String channelName;

    /**
     * 用户跟进销售人员ID
     */
    @Schema(description = "用户跟进销售人员ID")
    private Long saleId;

    /**
     * 用户跟进销售人员名
     */
    @Schema(description = "用户跟进销售人员名")
    private String saleName;

    /**
     * 用户微信名称
     */
    @Schema(description = "用户微信名称")
    private String wxName;

    /**
     * 当前激活的租户id
     */
    @Schema(description = "当前激活的租户id")
    private Long tenantId;

    /**
     * 代理商id
     */
    @Schema(description = "代理商id")
    private Long agentId;

    @Schema(description = "是否提交预约 0-否 1-是")
    private Integer isSubmitAppointment;
}
