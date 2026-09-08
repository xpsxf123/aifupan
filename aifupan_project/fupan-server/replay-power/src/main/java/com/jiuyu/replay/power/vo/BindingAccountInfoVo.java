package com.jiuyu.replay.power.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 父子绑定记录信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-31 10:18:16
 */
@Data
@Schema(description = "父子绑定记录信息项")
public class BindingAccountInfoVo extends BindingAccountVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
