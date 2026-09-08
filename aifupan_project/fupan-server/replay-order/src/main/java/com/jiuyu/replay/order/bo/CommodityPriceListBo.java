package com.jiuyu.replay.order.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品价格列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "商品价格列表查询参数")
public class CommodityPriceListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

    /**
     * 类型 0商品，1套餐
     */
    @Schema(description = "类型 0商品，1套餐")
    private Integer type = 1;

    /**
     * 商品id
     */
    @Schema(description = "商品id或版本id")
    private Long commodityId;

    /**
     * 显示状态 0官网不显示，1官网显示
     */
    @Schema(description = "显示状态 0官网不显示，1官网显示")
    private Integer showStatus;

    /**
     * 试用 0：不是，1：是
     */
    @Schema(description = "试用 0：不是，1：是")
    private Integer trialVersion;
}
