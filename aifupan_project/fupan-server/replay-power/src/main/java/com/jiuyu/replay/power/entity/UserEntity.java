package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-02-26 10:00:04
 */
@Data
@TableName("tb_user")
public class UserEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 登录账号
	 */
	private String username;
	/**
	 * 密码
	 */
	private String password;
	/**
	 * 昵称
	 */
	private String nickName;
	/**
	 * 手机号
	 */
	private String phone;
	/**
	 * 微信openid
	 */
	private String wxOpenid;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;
	/**
	 * 用户登录过的ip,前后用_隔开
	 */
	private String ips;

	/**
	 * 冻结状态 0：未冻结 1：已冻结
	 */
	private Integer status;

	/**
	 * 上级用户id
	 */
	private Long parentId;

	/**
	 *  用户类型 0：普通用户 1：后台管理员 2：子账号
	 */
	private Integer userType;
	/**
	 * 当前激活的租户id
	 */
	private Long activeTenantId;
	/**
	 * 邀请链接的code
	 */
	private String inviteUrlCode;
    /**
     * 后台管理员的用户类型(0：正常后台用户，1：代理商，2：代理商销售)
     */
    private Integer adminUserType;


}
