package com.jiuyu.replay.agent.vo;


import com.jiuyu.replay.generic.vo.common.FileShowVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 代理商信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@Schema(description = "代理商信息项")
public class AgentInfoVo extends AgentVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 海报图片信息列表
	 */
	@Schema(description = "海报图片信息列表")
	private List<FileShowVo> posterImgList;
	/**
	 * 行业名称
	 */
	@Schema(description = "行业名称")
	private String tradeName;
	/**
	 * 运营人员名称
	 */
	@Schema(description = "运营人员名称")
	private String operationUserName;
	/**
	 * 创建人名称
	 */
	@Schema(description = "创建人名称")
	private String createUserName;
	/**
	 * 链接地址
	 */
	@Schema(description = "链接地址")
	private String url;
}
