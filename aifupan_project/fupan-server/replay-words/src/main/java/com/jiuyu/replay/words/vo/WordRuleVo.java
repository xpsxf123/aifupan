package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 词语匹配规则信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Data
@Schema(description = "词语匹配规则信息")
public class WordRuleVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 词语id
	 */
	@Schema(description = "词语id")
	private Long wordId;
	/**
	 * 规则类型 0：在词语的左边范围内包含词语 1：在词语的右边范围内包含词语 2：在词语的两边任意一边范围内包含词语
	 */
	@Schema(description = "规则类型 0：在词语的左边范围内包含词语 1：在词语的右边范围内包含词语 2：在词语的两边任意一边范围内包含词语")
	private Integer type;
	/**
	 * 范围长度
	 */
	@Schema(description = "范围长度")
	private Integer rangeLength;
	/**
	 * 0：黑名单 1：白名单 2：限定词
	 */
	@Schema(description = "0：黑名单 1：白名单 2：限定词")
	private Integer blackOrWhite;
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
