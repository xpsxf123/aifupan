package com.jiuyu.replay.generic.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 字典信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@Data
@Schema(description = "字典信息")
public class DictDataVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 字典类型id
	 */
	@Schema(description = "字典类型id")
	private Long typeId;
	/**
	 * 字典label
	 */
	@Schema(description = "字典label")
	private String label;
	/**
	 * 字典value
	 */
	@Schema(description = "字典value")
	private String value;
	/**
	 * 状态 0：启用 1：禁用
	 */
	@Schema(description = "状态 0：启用 1：禁用")
	private Integer status;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;
    /**
     * 父id
     */
    @Schema(description = "父id")
    private Long parentId;
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
