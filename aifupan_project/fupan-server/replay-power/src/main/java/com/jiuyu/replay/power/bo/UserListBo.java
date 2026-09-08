package com.jiuyu.replay.power.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
@Schema
public class UserListBo extends PageBo {

    /**
     * 模糊搜索查询条件
     */
    @Schema(description = "模糊用户名称搜索查询条件")
    private String keyword;
    @Schema(description = "用户id")
    private Long  id;
    @Schema(description = "用户手机号码")
    private String  phone;
    @Schema(description = "父账号id")
    private Long  parentId;
    @Schema(description = "用户类型 0：普通用户 1：后台管理员 2：子账号")
    private Integer userType;
    @Schema(description = "用户名称")
    private String userName;
    @Schema(description = "用户昵称")
    private String  nickName;
    @Schema(description = "状态")
    private Integer status;

    /**
     * 主播唯一标识
     */
    @Schema(description = "主播唯一标识")
    private String  secUid;

    @Schema(description = "版本Id")
    private Long  packageId;

    @Schema(description = "公司名称")
    private String  companyName;

    @Schema(description = "公司Id")
    private List<Long> companyIds;

    @Schema(description = "行业")
    private String  tradeName;

    @Schema(description = "行业Id")
    private Long tradeId;

    @Schema(description = "用户id集合")
    private List<Long> userIds;

    @Schema(description = "注册结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @Schema(description = "注册开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;


    @Schema(description = "行业Ids")
    private List<Long> tradeIds;

    @Schema(description = "邀请链接的code")
    private List<String> inviteUrlCodes;

    @Schema(description = "自有账号大于等于多少")
    private Integer ownCount;

    @Schema(description = "是否是正式订单 1是试用 0是正式")
    private Integer trialOrder;

    @Schema(description = "是否已登陆过0未，1已登录过")
    private Integer isLoggedIn;


    /**
     * 到期时间
     */
    @Schema(description = "expireTime")
    private Long expireTime;

    /**
     * 销售人员ID
     */
    @Schema(description = "salesId")
    private Long salesId;

    /**
     * 日平均分析(查询的开始条件)
     */
    @Schema(description = "startDayAnalysis")
    private Long startDayAnalysis;

    /**
     * 日平均分析(查询的结束条件)
     */
    @Schema(description = "startDayAnalysis")
    private Long endDayAnalysis;

    /**
     * 多久未分析(天)(查询的开始条件)
     */
    @Schema(description = "startLongNotAnalysis")
    private Long startLongNotAnalysis;

    /**
     * 多久未分析(天)(查询的结束条件)
     */
    @Schema(description = "endLongNotAnalysis")
    private Long endLongNotAnalysis;

    /**
     * 用户微信名称
     */
    @Schema(description = "userWxName")
    private String userWxName;

    /**
     * 是否需要根据多久未分析、付费到期天数、总分析条数、多久未分析 升序或降序排序；如果等于1则是需要
     */
    @Schema(description = "specialSorting")
    private Integer specialSorting;

    /**
     * 升序或降序排序
     */
    @Schema(description = "searchUpOrDown")
    private Integer searchUpOrDown;

    /**
     * 渠道id
     */
    @Schema(description = "渠道id")
    private Long channelId;

    @Schema(description = "客户类型0个人、1工作室、2企业")
    private Integer userBelongType;

    @Schema(description = "用户意向S、A、B、C、D")
    private String userAmbition;

    @Schema(description = "渠道明细")
    private String promotionName;

    @Schema(description = "后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)")
    private Integer adminUserType;

    private Long agentId;

    @Schema(description = "销售归属 0平台销售，1代理商销售")
    private Integer salesType;

    @Schema(description = "员工状态 0-离职，1-在职")
    private Integer employeeStatus;

    @Schema(description = "排序字段 ownCount：自用账号数量, packageEndDate：版本过期时间, sumAnalysis：分析总数, dayAnalysis：日平均分析数, contrastAnalysis：对比分析条数")
    private String sortField;
    @Schema(description = "排序类型 asc升序，desc降序")
    private String sortOrder;

    @Schema(description = "客户端版本: record-纯录制版, replay-复盘版")
    private String clientVersion;

    @Schema(description = "是否提交预约 0-否 1-是")
    private Integer isSubmitAppointment;
}
