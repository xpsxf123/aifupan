package com.jiuyu.replay.words.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 客户端自定义词语列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:15:06
 */
@Data
@Schema(description = "客户端自定义词语列表查询参数")
public class SensitiveWordsClientListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 敏感词名字模糊搜索
	 */
	@Schema(description = "敏感词名字模糊搜索")
	private String keyword;
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
	 * 行业id，1表示全行业
	 */
	@Schema(description = "行业id，1表示全行业")
	private Long tradeId;
	/**
	 * 分组
	 */
	@Schema(description = "分组")
	private String groupStr;
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
	 * 按词语字数长度筛选
	 */
	@Schema(description = "按词语字数长度筛选")
	private Integer wordLength;
	/**
	 * 按关键词分类筛选
	 */
	@Schema(description = "按关键词分类筛选")
	private Long cruxTypeId;
	/**
	 * 按关键词分类id集合筛选
	 */
	@Schema(description = "按关键词分类id集合筛选")
	private List<Long> cruxTypeIds;
}
