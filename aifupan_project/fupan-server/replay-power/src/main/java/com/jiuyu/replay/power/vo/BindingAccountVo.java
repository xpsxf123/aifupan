package com.jiuyu.replay.power.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 父子绑定记录信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-31 10:18:16
 */
@Data
@Schema(description = "父子绑定记录信息")
public class BindingAccountVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * 父账号id
	 */
	@Schema(description = "父账号id")
	private Long parentUserId;
	/**
	 * 父账号名称
	 */
	@Schema(description = "父账号名称")
	private String parentUserName;
	/**
	 * 子账号id
	 */
	@Schema(description = "子账号id")
	private Long childUserId;
	/**
	 * 子账号名称
	 */
	@Schema(description = "子账号名称")
	private String childUserName;
	/**
	 * 绑定状态，0绑定，1解绑
	 */
	@Schema(description = "绑定状态，0绑定，1解绑")
	private Integer bindingStatus;
	/**
	 * 绑定时间
	 */
	@Schema(description = "绑定时间")
	private Date bindingDate;
	/**
	 * 解绑时间
	 */
	@Schema(description = "解绑时间")
	private Date unbindDate;
	/**
	 * 解绑原因
	 */
	@Schema(description = "解绑原因")
	private String unbindReason;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;


}
