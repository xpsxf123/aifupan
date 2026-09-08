package com.jiuyu.replay.generic.bo.power;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 新增用户信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-02-26 10:00:04
 */
@Data
@Schema(description = "新增用户信息")
public class AgentUserAddBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 登录账号
     */
    @Schema(description = "登录账号")
    private String username;
    /**
     * 密码
     */
    @Schema(description = "密码")
    private String password;
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
     * 后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)
     */
    @Schema(description = "后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)")
    private Integer adminUserType;

    /**
     * 角色id列表
     */
    @Schema(description = "角色id列表")
    private List<Long> roleIdList;

    /**
     * 行业id
     */
    private Long tradeId;

    /**
     * 用户来源渠道ID
     */
    private Long channelId;

    /**
     * 用户跟进销售人员ID
     */
    private Long saleId;

    /**
     * 代理商id
     */
    private Long agentId;

    /**
     * 冻结状态 0：未冻结 1：已冻结
     */
    private Integer status;
}
