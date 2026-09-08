package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class UserListVo extends UserVo {

    private String roleName;

    @Schema(description = "过去时间")
    private Date expirationDate;

    @Schema(description = "版本")
    private String packageName;

    @Schema(description = "版本id")
    private Long packageId;

    @Schema(description = "版本等级")
    private Integer packageLevel;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "公司id")
    private Long companyId;

    @Schema(description = "公司联系人")
    private String linkman;

    @Schema(description = "公司联系电话")
    private String phones;

    @Schema(description = "行业名称")
    private String tradeName;

    @Schema(description = "行业Id")
    private Long tradeId;

    @Schema(description = "用户微信名称")
    private String wxName;

    @Schema(description = "来源渠道id")
    private Long channelId;
    @Schema(description = "来源渠道")
    private String channelName;

    @Schema(description = "跟进销售Id")
    private String salesId;

    @Schema(description = "跟进销售")
    private String salesName;

    @Schema(description = "付费到期时间")
    private Long expireTime;

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
    /**
     * 代理商销售人员id
     */
    @Schema(description = "代理商销售人员id")
    private Long agentSaleId;
    /**
     * 代理商销售人员名称
     */
    @Schema(description = "代理商销售人员名称")
    private String agentSaleName;

    /**
     * 代理商推广渠道名称(渠道明细)
     */
    @Schema(description = "代理商推广渠道名称(渠道明细)")
    private String promotionName;

    /**
     * 代理商推广渠道id(渠道明细)
     */
    @Schema(description = "代理商推广渠道id(渠道明细)")
    private Long promotionId;

    @Schema(description = "用户意向S、A、B、C、D")
    private String userAmbition;

    @Schema(description = "客户类型0个人、1工作室、2企业")
    private Integer userBelongType;

    @Schema(description = "自有账号数量")
    private Integer ownCount;

    @Schema(description = "是否是正式版 1试用  0正式版")
    private Integer trialOrder;

    @Schema(description = "是否已登陆过0未，1已登录过")
    private Integer isLoggedIn;

    @Schema(description = "类型  0个人主播   1公司")
    private Integer anchorType;


    /**
     * 企业后台状态: -1不允许开通，0待开通， 1停用（过期），2正常, 3冻结
     */
    private Integer governanceStatus = -1;

    @Schema(description = "客户端版本: record-纯录制版, replay-复盘版")
    private String clientVersion;

    @Schema(description = "是否提交预约 0-否 1-是")
    private Integer isSubmitAppointment;

    /**
     * 用户累计算力消耗(token数)，空取 0L；仅 pageListNew 有值，其它端点为 null
     */
    @Schema(description = "用户累计算力消耗(token数)，T+1 离线值")
    private Long userPowerConsume;

    /**
     * 租户累计算力消耗(token数)，空取 0L；仅 pageListNew 有值，其它端点为 null
     */
    @Schema(description = "租户累计算力消耗(token数)，T+1 离线值")
    private Long tenantPowerConsume;

    /**
     * 最新一条跟进，无跟进为 null；仅 pageListNew 有值，其它端点为 null
     */
    @Schema(description = "最新一条跟进记录")
    private LatestRemarkVo latestRemark;

    /**
     * 用户近60天智能体会话数(用户提问条数)，外部接口实时装配，空/失败取 0L；仅 pageListNew 有值
     */
    @Schema(description = "用户近60天智能体会话数，实时值")
    private Long userAgentMessageCount;

    /**
     * 租户近60天智能体会话数，外部接口实时装配，空/失败取 0L；仅 pageListNew 有值
     * 注：租户维度为近似值——跨租户用户会在每个租户各计一次（见外部接口文档§5.4）
     */
    @Schema(description = "租户近60天智能体会话数，实时值")
    private Long tenantAgentMessageCount;

    /**
     * 用户近60天智能体计费token合计，外部接口实时装配，空/失败取 0L；仅 pageListNew 有值
     */
    @Schema(description = "用户近60天智能体Token消耗，实时值")
    private Long userAgentTokens;

    /**
     * 租户近60天智能体计费token合计，外部接口实时装配，空/失败取 0L；仅 pageListNew 有值
     */
    @Schema(description = "租户近60天智能体Token消耗，实时值")
    private Long tenantAgentTokens;

}
