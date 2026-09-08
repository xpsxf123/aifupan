package com.jiuyu.replay.third.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理ip提取
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@Data
@TableName("tb_proxy_ip")
public class ProxyIpEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 提取ip的url
	 */
	private String extractUrl;
	/**
	 * 总ip数
	 */
	private Integer totalNum;
	/**
	 * 剩余ip数
	 */
	private Integer remainingNum;
	/**
	 * 有效时长，分钟
	 */
	private Integer ipEffectiveTime;
	/**
	 * 激活状态 0：未已激活 1：已激活
	 */
	private Integer activeStatus;
	/**
	 * 代理IP账号
	 */
	private String proxyUsername;
	/**
	 * 代理IP密码
	 */
	private String proxyPassword;
	/**
	 * 时效类型 0：短效 1：长效
	 */
	private Integer validityType;
	/**
	 * 日使用次数
	 */
	private Integer dayUseNum;
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
