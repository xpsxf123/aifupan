package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 关键词信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:15:06
 */
@Data
@Schema(description = "关键词信息")
public class CruxWordsBatchBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
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
	 * 行业id，1表示全行业
	 */
	@Schema(description = "行业id，1表示全行业")
	private Long tradeId;
	/**
	 * 行业id数组json
	 */
	@Schema(description = "行业id数组json")
	private String tradeIdArr;
	/**
	 * 关键词列表
	 */
	@Schema(description = "关键词列表")
	private List<WordsBatchItemBo> wordsList;
	/**
	 * 分组
	 */
	@Schema(description = "分组")
	private String groupStr;

}
