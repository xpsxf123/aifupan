package com.jiuyu.replay.order.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品类型用量关联表列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "商品类型用量关联表列表查询参数")
public class TypeConsumptionListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

    /**
     * 商品类型id
     */
    @Schema(description = "商品类型id")
    private Long commodityTypeId;
    /**
     * 商品类型code
     */
    @Schema(description = "商品类型id")
    private String commodityTypeCode;
    /**
     * 商品类型名称
     */
    @Schema(description = "商品类型名称")
    private String commodityTypeName;
    /**
     * 来源id type为0字段为商品id，为1字段为订单详情id
     */
    @Schema(description = "来源id type为0字段为商品id，为1字段为订单详情id")
    private Long sourceId;
    /**
     * 来源类型 0商品，1套餐， 2邀请码
     */
    @Schema(description = "来源类型 0商品，1套餐， 2邀请码")
    private Integer type = 0;
}
