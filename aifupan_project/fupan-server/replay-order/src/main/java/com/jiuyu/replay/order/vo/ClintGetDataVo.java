package com.jiuyu.replay.order.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author lyw
 */
@Data
@Schema(description = "详细资产")
public class ClintGetDataVo  implements Serializable {

    private static final long serialVersionUID = 1L;


    @Schema(description = "订单详情id-tb_order_detail_id")
    private Long id;

    @Schema(description = "资产ID-订单定时器")
    private Long propertyId;

    @Schema(description = "商品类型0增量包，1套餐，2商品码")
    private Integer commodityType;


    @Schema(description = "订单id")
    private Long orderId;

    /**
     * 商品类型id
     */
    @Schema(description = "商品类型id")
    private Long commodityTypeId;
    /**
     * 商品类型key
     */
    @Schema(description = "商品类型key")
    private String commodityTypeCode;
    /**
     * 商品类型名称
     */
    @Schema(description = "商品类型名称")
    private String commodityTypeName;
    /**
     * 商品类型单位
     */
    @Schema(description = "商品类型单位")
    private String commodityTypeUnit;
    /**
     * 商品类型是否重置用量 0否，1是
     */
    @Schema(description = "商品类型是否重置用量 0否，1是")
    private Integer commodityTypeReset;
    /**
     * 用量
     */
    @Schema(description = "用量")
    private BigDecimal useNumber;
    /**
     * 剩余用量
     */
    @Schema(description = "剩余用量")
    private BigDecimal remUseNumber;
    /**
     * 总数量
     */
    @Schema(description = "总数量")
    private BigDecimal totalNumber;
    /**
     * 开始使用时间
     */
    @Schema(description = "开始使用时间")
    private Date startTime;
    /**
     * 结束使用时间
     */
    @Schema(description = "结束使用时间")
    private Date endTime;






}
