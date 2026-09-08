package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 版本商品信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "版本商品信息")
public class VersionCommodityBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "id")
    private Long id;

    /**
     * 版本id
     */
    @Schema(description = "版本id")
    @NotNull(message = "版本id不能为空")
    private Long sourceId;

    /**
     * 商品项id
     */
    @Schema(description = "商品项id [必填]")
    @NotNull(message = "商品项id不能为空")
    private Long commodityTypeId;

    /**
     * 数量
     */
    @Schema(description = "数量 [必填]")
    @NotNull(message = "数量不能为空")
    private Long number;
}

