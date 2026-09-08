package com.jiuyu.replay.third.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理ip提取记录信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@Data
@Schema(description = "代理ip提取记录信息")
public class ProxyIpRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 租户id
	 */
	@Schema(description = "租户id")
	private Long tenantId;
	/**
	 * 代理id
	 */
	@Schema(description = "代理id")
	private Long proxyId;
	/**
	 * ip地址
	 */
	@Schema(description = "ip地址")
	private String ipStr;
	/**
	 * 端口号
	 */
	@Schema(description = "端口号")
	private String portStr;
	/**
	 * 提取日期
	 */
	@Schema(description = "提取日期")
	private Date extractDate;
	/**
	 * 有效期
	 */
	@Schema(description = "有效期")
	private Date validityDate;
	/**
	 * ip有效时长，分钟
	 */
	@Schema(description = "ip有效时长，分钟")
	private Integer ipEffectiveTime;
	/**
	 * 代理IP账号
	 */
	@Schema(description = "代理IP账号")
	private String proxyUsername;
	/**
	 * 代理IP密码
	 */
	@Schema(description = "代理IP密码")
	private String proxyPassword;
	/**
	 * 时效类型 0：短效 1：长效
	 */
	@Schema(description = "时效类型 0：短效 1：长效")
	private Integer validityType;
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
