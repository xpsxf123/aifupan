package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统配置的键值对信息
 *
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
@Data
@Schema(description = "系统配置的键值对信息")
public class SystemKvVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * code
	 */
	@Schema(description = "code")
	private String kvKey;
	/**
	 * value
	 */
	@Schema(description = "value")
	private String kvValue;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remarks;
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
