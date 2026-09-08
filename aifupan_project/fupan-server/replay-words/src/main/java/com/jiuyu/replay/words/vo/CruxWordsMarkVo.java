package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 关键词信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
@Data
@Schema(description = "关键词信息")
public class CruxWordsMarkVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 来源类型 0：系统 1：客户自定义
	 */
	@Schema(description = "来源类型 0：系统 1：客户自定义")
	private Integer resourceType;
	/**
	 * 关键词类型 0：促单 1：互动 2：其他
	 */
	@Schema(description = "关键词类型 0：促单 1：互动 2：其他")
	private Integer type;
	/**
	 * 平台类型 0：全平台 1：抖音 2：快手 3：视频号
	 */
	@Schema(description = "平台类型 0：全平台 1：抖音 2：快手 3：视频号")
	private Integer platformType;
	/**
	 * 关键词名字
	 */
	@Schema(description = "关键词名字")
	private String name;
	/**
	 * 出现次数
	 */
	@Schema(description = "出现次数")
	private Integer countNum;

}
