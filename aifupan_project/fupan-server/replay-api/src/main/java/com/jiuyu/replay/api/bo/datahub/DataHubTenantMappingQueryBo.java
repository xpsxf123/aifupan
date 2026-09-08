package com.jiuyu.replay.api.bo.datahub;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Data Hub 客户→租户映射查询请求
 * phones 与 userIds 至少传一个，合计不超过 200
 */
@Data
@Schema(description = "Data Hub 客户→租户映射查询请求")
public class DataHubTenantMappingQueryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户手机号列表
     */
    @Size(max = 200, message = "phones数量不能超过200")
    @Schema(description = "手机号列表（与 userIds 至少传一个）")
    private List<String> phones;

    /**
     * 产品用户ID列表
     */
    @Size(max = 200, message = "userIds数量不能超过200")
    @Schema(description = "用户ID列表（与 phones 至少传一个）")
    private List<Long> userIds;
}
