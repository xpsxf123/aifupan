package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 词库
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-06 16:34:13
 */
@Data
@TableName("tb_lexicon")
public class LexiconEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 关联的用户id
	 */
	private Long userId;
	/**
	 * 关联的行业id
	 */
	private Long tradeId;
	/**
	 * 行业id数组json
	 */
	private String tradeIdArr;
	/**
	 * 词库名
	 */
	private String name;
	/**
	 * 词库备注
	 */
	private String remarks;
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
