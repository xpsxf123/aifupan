package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 客户端自定义词语信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
@Data
@Schema(description = "客户端自定义词语信息")
public class SensitiveWordsClientVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 规则列表
	 */
	@Schema(description = "规则列表")
	private List<WordRuleInfoVo> ruleList;
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
	 * 敏感词类型 0：广告 1：品牌 2：国家 3：限制词 4：其他    关键词类型 0：促单 1：互动 2：其他
	 */
	@Schema(description = "敏感词类型 0：广告 1：品牌 2：国家 3：限制词 4：其他    关键词类型 0：促单 1：互动 2：其他")
	private Integer type;
	/**
	 * 平台类型 0：全平台 1：抖音 2：快手 3：视频号
	 */
	@Schema(description = "平台类型 0：全平台 1：抖音 2：快手 3：视频号")
	private Integer platformType;
	/**
	 * 敏感词等级 0：1级,封号  1：2级,严重警告  2：3级警告
	 */
	@Schema(description = "敏感词等级 0：1级,封号  1：2级,严重警告  2：3级警告")
	private Integer level;
	/**
	 * 敏感词名字
	 */
	@Schema(description = "敏感词名字")
	private String name;
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
	 * 相似词，多个用_隔开
	 */
	@Schema(description = "相似词，多个用_隔开")
	private String similarWords;
	/**
	 * 父id
	 */
	@Schema(description = "父id")
	private Long parentId;
	/**
	 * 概览
	 */
	@Schema(description = "概览")
	private String overView;
	/**
	 * 描述
	 */
	@Schema(description = "描述")
	private String remarks;
	/**
	 * 分组
	 */
	@Schema(description = "分组")
	private String groupStr;
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
	 * 词语类型 0：敏感词 1：关键词 2：白名单(只有客户自定义有)
	 */
	@Schema(description = "词语类型 0：敏感词 1：关键词 2：白名单(只有客户自定义有)")
	private Integer wordsType;
	/**
	 * 状态 0：启用 1：禁用
	 */
	@Schema(description = "状态 0：启用 1：禁用")
	private Integer status;
	/**
	 * 关键词类型id
	 */
	@Schema(description = "关键词类型id")
	private Long cruxTypeId;


}
