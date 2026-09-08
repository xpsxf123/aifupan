package com.jiuyu.replay.order.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;

/**
 * 增量包表信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-09 16:13:50
 */
@Data
@Schema(description = "增量包表信息项")
public class IncrementInfoVo extends IncrementVo implements Serializable {
	private static final long serialVersionUID = 1L;


	@Schema(description = "商品")
	private CommodityInfoVo commodity;

	@Schema(description = "商品价格")
	private CommodityPriceVo commodityPrice;
}
