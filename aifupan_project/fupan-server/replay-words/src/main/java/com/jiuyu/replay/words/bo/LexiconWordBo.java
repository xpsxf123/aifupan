package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 词库-词语关联信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 10:41:43
 */
@Data
@Schema(description = "词库-词语关联信息")
public class LexiconWordBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 关联的词库id
	 */
	@Schema(description = "关联的词库id")
	private Long lexiconId;
	/**
	 * 关联的词语id
	 */
	@Schema(description = "关联的词语id")
	private Long wordId;
	/**
	 * 词语类型 0：敏感词 1：关键词 2：白名单
	 */
	@Schema(description = "词语类型 0：敏感词 1：关键词 2：白名单")
	private Integer wordsType;
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
