package com.jiuyu.replay.third.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理ip提取记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@Data
@TableName("tb_proxy_ip_record")
public class ProxyIpRecordEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 租户id
	 */
	private Long tenantId;
	/**
	 * 代理id
	 */
	private Long proxyId;
	/**
	 * ip地址
	 */
	private String ipStr;
	/**
	 * 端口号
	 */
	private String portStr;
	/**
	 * 提取日期
	 */
	private Date extractDate;
	/**
	 * 有效期
	 */
	private Date validityDate;
	/**
	 * ip有效时长，分钟
	 */
	private Integer ipEffectiveTime;
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
