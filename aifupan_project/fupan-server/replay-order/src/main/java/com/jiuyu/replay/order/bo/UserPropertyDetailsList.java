package com.jiuyu.replay.order.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/22 10:30
 */
@Data
@Schema(description = "用户资产列表查询参数")
public class UserPropertyDetailsList extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    @NotNull(message = "用户id不能为空")
    private Long userId;

    @Schema(description = "商品类型编码 aiAnalysisTime:智能分析时长, aiTokenNum:Ai算力")
    private String commodityTypeCode;

    private Long propertyId;

    private Date startDate;

    private Date endDate;

}
