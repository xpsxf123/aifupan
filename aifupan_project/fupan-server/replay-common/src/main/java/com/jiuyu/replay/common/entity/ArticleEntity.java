package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文章
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-23 20:39:45
 */
@Data
@TableName("tb_article")
public class ArticleEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 创建人id
	 */
	private Long userId;
	/**
	 * 文章标题
	 */
	private String title;
	/**
	 * 备注
	 */
	private String remarks;
	/**
	 * 类型
	 */
	private Integer type;
	/**
	 * 富文本内容
	 */
	private String content;
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
