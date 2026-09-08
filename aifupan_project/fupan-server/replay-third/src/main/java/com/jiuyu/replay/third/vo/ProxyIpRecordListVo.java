package com.jiuyu.replay.third.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 代理ip提取记录列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@Data
@Schema(description = "代理ip提取记录列表项")
public class ProxyIpRecordListVo extends ProxyIpRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户名称
	 */
	@Schema(description = "用户名称")
	private String userNickName;

}
