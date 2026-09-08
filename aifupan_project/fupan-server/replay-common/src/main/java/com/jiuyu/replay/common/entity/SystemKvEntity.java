package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统配置的键值对
 *
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
@Data
@TableName("tb_system_kv")
public class SystemKvEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * code
	 */
	private String kvKey;
	/**
	 * value
	 */
	private String kvValue;
	/**
	 * 备注
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
