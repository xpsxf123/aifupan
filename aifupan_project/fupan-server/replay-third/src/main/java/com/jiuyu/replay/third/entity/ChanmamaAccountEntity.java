package com.jiuyu.replay.third.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 第三方数据平台账号
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-14 15:59:03
 */
@Data
@TableName("tb_chanmama_account")
public class ChanmamaAccountEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 第三方数据平台账号
	 */
	private String username;
	/**
	 * 第三方数据平台密码
	 */
	private String chanmamaPassword;
	/**
	 * 每天能查询的次数
	 */
	private Integer everyDayQueryNum;
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


}
