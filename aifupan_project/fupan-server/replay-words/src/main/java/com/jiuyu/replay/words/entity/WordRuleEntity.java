package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 词语匹配规则
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Data
@TableName("tb_word_rule")
public class WordRuleEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 词语id
	 */
	private Long wordId;
	/**
	 * 规则类型 0：在词语的左边范围内包含词语 1：在词语的右边范围内包含词语 2：在词语的两边任意一边范围内包含词语
	 */
	private Integer type;
	/**
	 * 范围长度
	 */
	private Integer rangeLength;
	/**
	 * 0：黑名单 1：白名单 2：限定词
	 */
	private Integer blackOrWhite;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;


}
