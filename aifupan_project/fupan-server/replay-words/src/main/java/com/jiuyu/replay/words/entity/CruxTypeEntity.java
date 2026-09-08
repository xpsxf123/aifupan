package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 关键词类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Data
@TableName("tb_crux_type")
public class CruxTypeEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 关键词类型名称
	 */
	private String name;
	/**
	 * 层级，从1开始
	 */
	private Integer level;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * tab的排序
	 */
	private Integer tabSort;
	/**
	 * 父id
	 */
	private Long parentId;
	/**
	 * 是否在数据罗盘展示 0：否 1：是
	 */
	private Integer isShowCompass;
	/**
	 * 是否统计到关键词总数 0：否 1：是
	 */
	private Integer isCount;
	/**
	 * 描述
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
