package com.jiuyu.replay.generic.vo.power;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "菜单")
public class MenuVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 父菜单id
	 */
	@Schema(description = "父菜单id")
	private Long parentId;
	/**
	 * 菜单名
	 */
	@Schema(description = "菜单名")
	private String name;
	/**
	 * 菜单url
	 */
	@Schema(description = "菜单url")
	private String url;
	/**
	 * 0：菜单 1：功能 2：目录
	 */
	@Schema(description = "0：菜单 1：功能 2：目录")
	private Integer type;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;
	/**
	 * 图标
	 */
	@Schema(description = "图标")
	private String img;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;


}
