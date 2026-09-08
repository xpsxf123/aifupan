package com.jiuyu.replay.api.bo.datahub;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Data Hub 订单增量对账查询请求
 */
@Data
@Schema(description = "Data Hub 订单增量对账查询请求")
public class DataHubOrderReconQueryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID列表，可空 = 不限租户
     */
    @Size(max = 200, message = "tenantIds数量不能超过200")
    @Schema(description = "租户ID列表（可空 = 不限租户）")
    private List<Long> tenantIds;

    /**
     * 增量起点（含），按订单 update_date，格式 yyyy-MM-dd HH:mm:ss，可空
     */
    @Schema(description = "增量起点（含），yyyy-MM-dd HH:mm:ss，可空")
    private String updatedAfter;

    /**
     * 游标（上一页响应的 nextCursor 原样回传），可空 = 第一页
     */
    @Schema(description = "游标，第一页不传")
    private String cursor;

    /**
     * 每页条数，1~500，默认 200
     */
    @Min(value = 1, message = "pageSize最小为1")
    @Max(value = 500, message = "pageSize最大为500")
    @Schema(description = "每页条数，1~500，默认 200")
    private Integer pageSize;
}
