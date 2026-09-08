package com.jiuyu.replay.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 字典
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:29:05
 */
@Data
@TableName("tb_dict_data")
public class DictDataEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 字典类型id
	 */
	private Long typeId;
	/**
	 * 字典label
	 */
	private String label;
	/**
	 * 字典value
	 */
	private String value;
	/**
	 * 状态 0：启用 1：禁用
	 */
	private Integer status;
	/**
	 * 排序
	 */
	private Integer sort;
    /**
     * 父id
     */
    private Long parentId;
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
