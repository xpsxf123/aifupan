package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/9 下午5:21
 */
@Data
public class ActivityCommodityBo extends CommodityTypeBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品数据量（当奖励类型为增加包时有）")
    private Long commodityNumber;

    @Schema(description = "商品有效期值，跟validity_unit结合使用，如：3年")
    private Integer validityNum;

    @Schema(description = "商品有效期单位，0：小时 1：天 2：月 3：季度 4：半年，5：年")
    private Integer validityUnit;

}
