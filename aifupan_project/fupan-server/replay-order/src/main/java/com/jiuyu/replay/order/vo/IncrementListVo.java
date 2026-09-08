package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 增量包表列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-09 16:13:50
 */
@Data
@Schema(description = "增量包表列表项")
public class IncrementListVo extends IncrementVo implements Serializable {
	private static final long serialVersionUID = 1L;

    @Schema(description = "商品信息")
    private CommodityInfoVo commodityVo;

    @Schema(description = "商品价格")
    private CommodityPriceVo commodityPrice;
}
