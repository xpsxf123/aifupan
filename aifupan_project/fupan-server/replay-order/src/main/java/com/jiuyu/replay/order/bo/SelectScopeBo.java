package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "查询范围参数")
public class SelectScopeBo {

    @Schema(description = "商品类型code")
    private String code;

    @Schema(description = "最小值")
    private Integer min;

    @Schema(description = "最大值")
    private Integer max;

}
