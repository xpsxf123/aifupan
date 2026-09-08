package com.jiuyu.replay.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理商平台销售信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Data
@Schema(description = "代理商平台销售信息")
public class AgentPlatformSaleVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 代理商id
	 */
	@Schema(description = "代理商id")
	private Long agentId;
	/**
	 * 销售人员id
	 */
	@Schema(description = "销售人员id")
	private Long saleId;
	/**
	 * 渠道二维码图片文件id
	 */
	@Schema(description = "渠道二维码图片文件id")
	private Long channelQrcodeImgId;
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
