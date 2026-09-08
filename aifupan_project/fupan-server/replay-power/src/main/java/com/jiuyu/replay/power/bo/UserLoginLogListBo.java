package com.jiuyu.replay.power.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录日志列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-15 18:48:54
 */
@Data
@Schema(description = "用户登录日志列表查询参数")
public class UserLoginLogListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "用户id")
	private Long userId;

	@Schema(description = "用户名称")
	private String userName;

	@Schema(description = "用户类型")
	private Integer userType;

	@Schema(description = "操作类型，0登录 1离线 2上线 3退出")
	private Integer operaType;

	@Schema(description = "操作状态状态（0成功 1失败）")
	private Integer operaStatus;

	@Schema(description = "IP地址（IPv4 / IPv6）")
	private String ipAddress;
}
