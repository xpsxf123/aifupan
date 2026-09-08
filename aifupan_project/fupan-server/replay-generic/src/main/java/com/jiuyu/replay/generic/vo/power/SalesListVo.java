package com.jiuyu.replay.generic.vo.power;

import com.jiuyu.replay.generic.vo.common.FileShowVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户跟进销售人员表列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:13
 */
@Data
@Schema(description = "用户跟进销售人员表列表项")
public class SalesListVo extends SalesVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 二维码图片信息
	 */
	@Schema(description = "二维码图片信息")
	private FileShowVo qrcodeImgInfo;

    /**
     * 代理商名称
     */
    @Schema(description = "代理商名称")
    private String agentName;

}
