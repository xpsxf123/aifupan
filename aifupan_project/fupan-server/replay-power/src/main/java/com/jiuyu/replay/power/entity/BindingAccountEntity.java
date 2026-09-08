package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 父子绑定记录
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-31 10:18:16
 */
@Data
@TableName("tb_binding_account")
public class BindingAccountEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 父账号id
	 */
	private Long parentUserId;
	/**
	 * 父账号名称
	 */
	private String parentUserName;
	/**
	 * 子账号id
	 */
	private Long childUserId;
	/**
	 * 子账号名称
	 */
	private String childUserName;
	/**
	 * 绑定状态，0绑定，1解绑
	 */
	private Integer bindingStatus;
	/**
	 * 绑定时间
	 */
	private Date bindingDate;
	/**
	 * 解绑时间
	 */
	private Date unbindDate;
	/**
	 * 解绑原因
	 */
	private String unbindReason;
	/**
	 * 创建时间
	 */
	private Date createDate;


}
