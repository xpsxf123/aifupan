package com.jiuyu.replay.power.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户登录日志列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-15 18:48:54
 */
@Data
@Schema(description = "用户登录日志列表项")
public class UserLoginLogListVo extends UserLoginLogVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
