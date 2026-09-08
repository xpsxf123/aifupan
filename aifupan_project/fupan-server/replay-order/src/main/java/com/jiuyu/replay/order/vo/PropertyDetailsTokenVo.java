package com.jiuyu.replay.order.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 资产使用记录关联aitoken消耗表信息
 *
 * @author lj
 * @email 
 * @date 2025-03-21 18:45:03
 */
@Data
@Schema(description = "资产使用记录关联aitoken消耗表信息")
public class PropertyDetailsTokenVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * aiTokenId记录id
	 */
	@Schema(description = "aiTokenId记录id")
	private Long aiTokenId;
	/**
	 * 资产记录id
	 */
	@Schema(description = "资产记录id")
	private Long propertyDetailsId;


}
