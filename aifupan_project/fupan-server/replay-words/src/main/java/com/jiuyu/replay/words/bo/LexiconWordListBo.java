package com.jiuyu.replay.words.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 词库-词语关联列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 10:41:43
 */
@Data
@Schema(description = "词库-词语关联列表查询参数")
public class LexiconWordListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 根据词语名称模糊查询
	 */
	@Schema(description = "根据词语名称模糊查询")
	private String name;
	/**
	 * 词库id
	 */
	@Schema(description = "词库id")
	private String lexiconId;
	/**
	 * 词语类型
	 */
	@Schema(description = "词语类型")
	private Integer wordType;
	/**
	 * 添加词语的开始时间
	 */
	@Schema(description = "添加词语的开始时间")
	private String startTime;
	/**
	 * 添加词语的结束时间
	 */
	@Schema(description = "添加词语的结束时间")
	private String endTime;
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
