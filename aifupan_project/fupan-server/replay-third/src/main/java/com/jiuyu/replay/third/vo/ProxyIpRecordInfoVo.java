package com.jiuyu.replay.third.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 代理ip提取记录信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@Data
@Schema(description = "代理ip提取记录信息项")
public class ProxyIpRecordInfoVo extends ProxyIpRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * IP过期的时间戳
	 */
	@Schema(description = "IP过期的时间戳")
	private Long expireTime;

}
