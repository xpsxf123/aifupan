package com.jiuyu.replay.third.vo.chanmama;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 第三方数据平台账号信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-14 15:59:03
 */
@Data
@Schema(description = "第三方数据平台账号信息")
public class ChanmamaAccountVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 第三方数据平台账号
	 */
	@Schema(description = "第三方数据平台账号")
	private String username;
	/**
	 * 第三方数据平台密码
	 */
	@Schema(description = "第三方数据平台密码")
	private String chanmamaPassword;
	/**
	 * 每天能查询的次数
	 */
	@Schema(description = "每天能查询的次数")
	private String everyDayQueryNum;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
