package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 相似达人每日汇总
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-10-16 15:40:25
 */
@Deprecated
@Data
@TableName("tb_similar_collect")
public class SimilarCollectEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 行业id
	 */
	private Long tradeId;
	/**
	 * 相似达人id
	 */
	private Long similarAnchorId;
	/**
	 * 相似达人secUid
	 */
	private String similarSecUid;
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
