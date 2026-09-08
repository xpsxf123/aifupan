package com.jiuyu.replay.power.vo.datahub;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Data Hub 成员数据项（模块内传输对象）
 * 承载 tb_user + tb_user_details.is_logged_in 的成员快照，供 replay-api 数据开放层组装对外响应
 */
@Data
@Schema(description = "Data Hub 成员数据项")
public class DataHubMemberVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long id;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickName;

    /**
     * 用户类型 0普通用户(主账号) 1后台管理员 2子账号
     */
    @Schema(description = "用户类型 0普通用户 1后台管理员 2子账号")
    private Integer userType;

    /**
     * 上级用户id
     */
    @Schema(description = "上级用户id")
    private Long parentId;

    /**
     * 当前激活租户id
     */
    @Schema(description = "当前激活租户id")
    private Long activeTenantId;

    /**
     * 冻结状态 0未冻结 1已冻结
     */
    @Schema(description = "冻结状态 0未冻结 1已冻结")
    private Integer status;

    /**
     * 是否已删除 0否 1是
     */
    @Schema(description = "是否已删除 0否 1是")
    private Integer isDeleted;

    /**
     * 是否登录过 0否 1是（来自 tb_user_details）
     */
    @Schema(description = "是否登录过 0否 1是")
    private Integer isLoggedIn;

    /**
     * 注册时间
     */
    @Schema(description = "注册时间")
    private Date createDate;

    /**
     * 最后更新时间
     */
    @Schema(description = "最后更新时间")
    private Date updateDate;
}
