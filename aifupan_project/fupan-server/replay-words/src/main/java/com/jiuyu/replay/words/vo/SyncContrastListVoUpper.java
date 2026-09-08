package com.jiuyu.replay.words.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVoUpper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户端对比数据列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
@Data
@Schema(description = "客户端对比数据列表项")
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SyncContrastListVoUpper extends SyncContrastVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主播一信息
	 */
	@Schema(description = "主播一信息")
	private AnchorUrlInfoVoUpper anchorInfoOne;
	/**
	 * 主播二信息
	 */
	@Schema(description = "主播二信息")
	private AnchorUrlInfoVoUpper anchorInfoTwo;
	/**
	 * 视频一信息
	 */
	@Schema(description = "视频一信息")
	private AnchorVideoInfoVoUpper videoInfoOne;
	/**
	 * 视频二信息
	 */
	@Schema(description = "视频二信息")
	private AnchorVideoInfoVoUpper videoInfoTwo;
	/**
	 * 上传用户的名称
	 */
	@Schema(description = "上传用户的名称")
	private String userNickName;

}
