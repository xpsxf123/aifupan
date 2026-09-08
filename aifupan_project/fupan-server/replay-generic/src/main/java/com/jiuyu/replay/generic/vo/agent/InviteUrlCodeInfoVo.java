package com.jiuyu.replay.generic.vo.agent;


import com.jiuyu.replay.generic.vo.common.FileShowVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 邀请链接的code信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@Schema(description = "邀请链接的code信息项")
public class InviteUrlCodeInfoVo extends InviteUrlCodeVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 销售人员id
	 */
	@Schema(description = "销售人员id")
	private Long saleId;
	/**
	 * 销售二维码图片
	 */
	@Schema(description = "销售二维码图片")
	private FileShowVo saleQrcodeImg;
	/**
	 * 代理商id
	 */
	@Schema(description = "代理商id")
	private Long agentId;
	/**
	 * 代理商海报图片信息列表
	 */
	@Schema(description = "代理商海报图片信息列表")
	private List<FileShowVo> posterImgList;
	/**
	 * 代理商按钮颜色
	 */
	@Schema(description = "代理商按钮颜色")
	private String btnBgColor;
	/**
	 * 按钮文案
	 */
	@Schema(description = "按钮文案")
	private String btContent;


}
