package com.jiuyu.replay.power.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 父子绑定记录列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-31 10:18:16
 */
@Data
@Schema(description = "父子绑定记录列表项")
public class BindingAccountListVo extends BindingAccountVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
