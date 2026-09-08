package com.jiuyu.governance.openfeign.replay.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 账户详细信息
 *
 * @author HeHui
 * @date 2026-03-30 10:46
 */
@Getter
@Setter
public class ReplayAccountDetailResponse {

    /**
     * id
     */
    private Long id;
    /**
     * tb_user 的 id
     */
    private Long userId;
    /**
     * 行业 id
     */
    private Long tradeId;
    /**
     * 公司表 id
     */
    private Long companyId;
    /**
     * 类型  0 个人主播   1 公司
     */
    private Integer anchorType;
    /**
     * 公司所在职位
     */
    private String position;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 身份证号
     */
    private String idCard;
    /**
     * 真实名称
     */
    private String realName;
    /**
     * 性别 1 男 2 女
     */
    private Integer sex;
    /**
     * 出生年月日
     */
    private Date birthday;
    /**
     * 现用户地址
     */
    private String address;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 更新时间
     */
    private Date updateDate;
    /**
     * 是否删除（0 未删除 1 删除）
     */
    private Integer isDeleted;

    /**
     * 用户来源渠道 ID
     */
    private Long channelId;

    /**
     * 用户跟进销售人员 ID
     */
    private Long saleId;

    /**
     * 用户微信名称
     */
    private String wxName;

    /**
     * 代理商销售 ID
     */
    private Long agentSaleId;
    /**
     * 代理商销售名称
     */
    private String agentSaleName;
    /**
     * 是否演示
     */
    private Integer isShow;

    /**
     * 视频会议地址
     */
    private String videoMeetPath;
    /**
     * 用户意向
     */
    private String userAmbition;
    /**
     * 客户类型 0 个人、1 工作室、2 企业
     */
    private Integer userBelongType;
    /**
     * 是否登录过 0 未  1 已登录过
     */
    private Integer isLoggedIn;

    /**
     * 代理商 id
     */
    private Long agentId;


    /**
     * 行业名称
     */
    private Long tradeName;

    /**
     * 父用户 id
     */
    private Long parentId;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户注册时间
     */
    private Date registerDate;

    /**
     * 冻结状态 0：未冻结 1：已冻结
     */
    private Integer userStatus;

    /**
     * 版本等级
     */
    private Integer packageLevel;

    /**
     * 版本名称
     */
    private String packageName;

    /**
     * 版本过期时间
     */
    private Date expirationDate;

    /**
     * 公司信息
     */
    private CompanyResponse company;


    /**
     * 用户来源渠道名
     */
    private String channelName;


    /**
     * 用户跟进销售人员名
     */
    private String saleName;



    /**
     * 当前激活的租户 id
     */
    private Long tenantId;


    /**
     * 密码
     */
    private String password;
}
