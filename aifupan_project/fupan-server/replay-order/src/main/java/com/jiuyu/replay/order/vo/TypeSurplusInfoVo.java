package com.jiuyu.replay.order.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 商品类型资产剩余表信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@Schema(description = "商品类型资产剩余表信息项")
public class TypeSurplusInfoVo extends TypeSurplusVo implements Serializable {
	private static final long serialVersionUID = 1L;


	@Schema(description = "商品类型等级")
	private Integer commodityTypeLevel;

	@Schema(description = "商品类型 0增量包，1套餐，2邀请码")
	private Integer commodityType;

}
