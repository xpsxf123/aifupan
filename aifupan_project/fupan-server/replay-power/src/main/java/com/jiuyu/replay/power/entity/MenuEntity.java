package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 菜单
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-02-26 10:00:04
 */
@Data
@TableName("tb_menu")
public class MenuEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 父菜单id
	 */
	private Long parentId;
	/**
	 * 菜单名
	 */
	private String name;
	/**
	 * 菜单url
	 */
	private String url;
	/**
	 * 0：菜单 1：功能 2：目录
	 */
	private Integer type;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 图标
	 */
	private String img;
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
