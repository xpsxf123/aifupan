package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/10/30 17:03
 */
@Data
public class ServerUserListVo {

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 登录账号
     */
    @Schema(description = "登录账号")
    private String username;
    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickName;
    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;
    /**
     * 用户登录过的ip,前后用_隔开
     */
    @Schema(description = "用户登录过的ip,前后用_隔开")
    private String ips;
    /**
     * 冻结状态 0：未冻结 1：已冻结
     */
    @Schema(description = "冻结状态 0：未冻结 1：已冻结")
    private Integer status;
    /**
     * 上级用户id
     */
    @Schema(description = "上级用户id")
    private Long parentId;
    /**
     * 用户类型 0：普通用户 1：后台管理员 2：子账号
     */
    @Schema(description = "用户类型 0：普通用户 1：后台管理员 2：子账号")
    private Integer userType;
    /**
     * 当前激活的租户id
     */
    @Schema(description = "当前激活的租户id")
    private Long activeTenantId;
    /**
     * 邀请链接的code
     */
    @Schema(description = "邀请链接的code")
    private String inviteUrlCode;
    /**
     * 后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)
     */
    @Schema(description = "后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)")
    private Integer adminUserType;

    @Schema(description = "跟进销售Id")
    private String salesId;

    @Schema(description = "类型  0个人主播   1公司")
    private Integer anchorType;

    @Schema(description = "版本id")
    private Long packageId;

    @Schema(description = "用户微信名称")
    private String wxName;

    @Schema(description = "是否已登陆过0未，1已登录过")
    private Integer isLoggedIn;

    @Schema(description = "行业Id")
    private Long tradeId;

    @Schema(description = "自有账号数量")
    private Integer ownCount;

    @Schema(description = "用户意向S、A、B、C、D")
    private String userAmbition;

    @Schema(description = "客户类型0个人、1工作室、2企业")
    private Integer userBelongType;

    @Schema(description = "是否是正式版 1试用  0正式版")
    private Integer trialOrder;

    @Schema(description = "版本过期时间")
    private Date expirationDate;

    @Schema(description = "来源渠道id")
    private Long channelId;

    @Schema(description = "分析总数")
    private Long sumAnalysis;

    @Schema(description = "日平均分析数")
    private Long dayAnalysis;

    @Schema(description = "最后一次分析时间")
    private Date lastAnalysis;

    @Schema(description = "多久未分析")
    private Long longNotAnalysis;

    @Schema(description = "用户对比分析条数")
    private Long contrastAnalysis;

    @Schema(description = "代理商推广渠道名称(渠道明细)")
    private String promotionName;

    @Schema(description = "客户端版本: record-纯录制版, replay-复盘版")
    private String clientVersion;

    @Schema(description = "是否提交预约 0-否 1-是")
    private Integer isSubmitAppointment;

    /**
     * 用户累计算力消耗(token数)，SQL LEFT JOIN tb_user_power_rollup 带出，未命中为 null
     */
    @Schema(description = "用户累计算力消耗(token数)")
    private Long userPowerConsume;

    /**
     * 租户累计算力消耗(token数)，SQL LEFT JOIN tb_tenant_power_rollup 带出，未命中为 null
     */
    @Schema(description = "租户累计算力消耗(token数)")
    private Long tenantPowerConsume;

    /**
     * 用户近60天智能体会话数(用户提问条数)，外部智能体统计接口实时装配，失败/无数据兜底0
     */
    @Schema(description = "用户近60天智能体会话数")
    private Long userAgentMessageCount;

    /**
     * 租户近60天智能体会话数，外部接口实时装配；租户维度为近似值(跨租户用户重复计数)，失败/无数据兜底0
     */
    @Schema(description = "租户近60天智能体会话数")
    private Long tenantAgentMessageCount;

    /**
     * 用户近60天智能体计费token合计，外部智能体统计接口实时装配，失败/无数据兜底0
     */
    @Schema(description = "用户近60天智能体Token消耗")
    private Long userAgentTokens;

    /**
     * 租户近60天智能体计费token合计，外部接口实时装配，失败/无数据兜底0
     */
    @Schema(description = "租户近60天智能体Token消耗")
    private Long tenantAgentTokens;
}
