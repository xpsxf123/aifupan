package com.jiuyu.replay.agent.vo;


import com.jiuyu.replay.generic.vo.common.FileShowVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 代理商平台销售信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:28
 */
@Data
@Schema(description = "代理商平台销售信息项")
public class AgentPlatformSaleInfoVo extends AgentPlatformSaleVo implements Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * 渠道二维码图片
	 */
	@Schema(description = "渠道二维码图片")
	private FileShowVo channelQrcodeImg;

	/**
	 * 销售人员名称
	 */
	@Schema(description = "销售人员名称")
	private String salesName;
	/**
	 * 销售人员手机号
	 */
	@Schema(description = "销售人员手机号")
	private String phone;

    /**
     * 销售人员推广码
     */
    @Schema(description = "销售人员推广码")
    private String url;

    @Schema(description = "销售类型 0平台销售，1代理商销售")
    private Integer salesType;
}
