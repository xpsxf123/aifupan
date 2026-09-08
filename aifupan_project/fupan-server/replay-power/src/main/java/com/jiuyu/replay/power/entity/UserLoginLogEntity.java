package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户登录日志
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-15 18:48:54
 */
@Data
@TableName("tb_user_login_log")
public class UserLoginLogEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 用户名称
	 */
	private String userName;
	/**
	 * 用户类型
	 */
	private Integer userType;
	/**
	 * 操作类型，0登录 1离线 2上线 3退出
	 */
	private Integer operaType;
	/**
	 * IP地址（IPv4 / IPv6）
	 */
	private String ipAddress;
	/**
	 * 操作状态状态（0成功 1失败）
	 */
	private Integer operaStatus;
	/**
	 * 备注（例如：密码错误、账号锁定等）
	 */
	private String remarks;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 修改时间
	 */
	private Date updateDate;
	/**
	 * 删除标志
	 */
	private Integer isDeleted;


}
