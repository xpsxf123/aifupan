package com.jiuyu.governance.business.rbac.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 爱复盘绑定子账户请求回调
 *
 * @author HeHui
 * @date 2026-04-16 22:01
 */
@Getter
@Setter
public class BindSubAccountRequest {

    /**
     * info表
     */
    private Long id;
    /**
     * tb_user的id
     */
    @NotNull(message = "用户id不能为空")
    private Long userId;
    /**
     * 行业id
     */
    private Long tradeId;
    /**
     * 公司表id
     */
    private Long companyId;
    /**
     * 类型  0个人主播   1公司
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
     * 真实名称
     */
    private String realName;
    /**
     * 性别 1男 2女
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
     * 用户来源渠道ID
     */
    private Long channelId;

    /**
     * 用户跟进销售人员ID
     */
    private Long saleId;

    /**
     * 用户微信名称
     */
    private String wxName;

    /**
     * 代理商销售ID
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
     * 是否演示
     */
    private String videoMeetPath;
    /**
     * 用户意向
     */
    private String userAmbition;
    /**
     * 客户类型0个人、1工作室、2企业
     */
    private Integer userBelongType;
    /**
     * 是否登录过 0未  1已登录过
     */
    private Integer isLoggedIn;

    /**
     * 代理商id
     */
    private Long agentId;


    /**
     * 上级id
     */
    private Long parentId;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 密码
     */
    private String password;

    /**
     * 注册时间
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
     * 版本到期时间
     */
    private Date expirationDate;

    /**
     * 租户id
     */
    @NotNull(message = "租户id不能为空")
    private Long tenantId;
}
