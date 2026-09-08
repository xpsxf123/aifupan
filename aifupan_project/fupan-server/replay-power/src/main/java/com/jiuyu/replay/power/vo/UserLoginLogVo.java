package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户登录日志信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-15 18:48:54
 */
@Data
@Schema(description = "用户登录日志信息")
public class UserLoginLogVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 用户名称
	 */
	@Schema(description = "用户名称")
	private String userName;
	/**
	 * 用户类型
	 */
	@Schema(description = "用户类型")
	private Integer userType;
	/**
	 * 操作类型，0登录 1离线 2上线 3退出
	 */
	@Schema(description = "操作类型，0登录 1离线 2上线 3退出")
	private Integer operaType;
	/**
	 * IP地址（IPv4 / IPv6）
	 */
	@Schema(description = "IP地址（IPv4 / IPv6）")
	private String ipAddress;
	/**
	 * 操作状态状态（0成功 1失败）
	 */
	@Schema(description = "操作状态状态（0成功 1失败）")
	private Integer operaStatus;
	/**
	 * 备注（例如：密码错误、账号锁定等）
	 */
	@Schema(description = "备注（例如：密码错误、账号锁定等）")
	private String remarks;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 修改时间
	 */
	@Schema(description = "修改时间")
	private Date updateDate;
	/**
	 * 删除标志
	 */
	@Schema(description = "删除标志")
	private Integer isDeleted;


}
