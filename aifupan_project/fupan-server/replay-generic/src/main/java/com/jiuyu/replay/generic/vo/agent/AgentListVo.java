package com.jiuyu.replay.generic.vo.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 代理商列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@Schema(description = "代理商列表项")
public class AgentListVo extends AgentVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 运营人员
	 */
	@Schema(description = "运营人员")
	private String operationUserName;
	/**
	 * 行业名称
	 */
	@Schema(description = "行业名称")
	private String tradeName;
	/**
	 * 创建人
	 */
	@Schema(description = "创建人")
	private String createUserName;
	/**
	 * 链接地址
	 */
	@Schema(description = "链接地址")
	private String url;
}
