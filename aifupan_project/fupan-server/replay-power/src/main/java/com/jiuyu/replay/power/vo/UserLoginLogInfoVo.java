package com.jiuyu.replay.power.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 用户登录日志信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-15 18:48:54
 */
@Data
@Schema(description = "用户登录日志信息项")
public class UserLoginLogInfoVo extends UserLoginLogVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
