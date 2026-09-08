package com.jiuyu.replay.api.vo.datahub;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data Hub 客户→租户映射响应
 */
@Data
@Schema(description = "Data Hub 客户→租户映射响应")
public class DataHubTenantMappingVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 映射结果列表（与入参一一对应）
     */
    @Schema(description = "映射结果列表")
    private List<Item> items = new ArrayList<>();

    /**
     * 单条映射结果
     */
    @Data
    @Schema(description = "单条映射结果")
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 回显入参手机号（按 phone 查询时返回）
         */
        @Schema(description = "回显入参手机号")
        private String phone;

        /**
         * 产品用户ID
         */
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "产品用户ID")
        private Long userId;

        /**
         * 当前激活租户ID
         */
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "当前激活租户ID")
        private Long tenantId;

        /**
         * 关系角色 MAIN主账号 / SUB子账号
         */
        @Schema(description = "关系角色 MAIN / SUB")
        private String relationStatus;

        /**
         * 账号状态 NORMAL正常 / FROZEN冻结
         */
        @Schema(description = "账号状态 NORMAL / FROZEN")
        private String accountStatus;

        /**
         * 缺失原因：null=命中 / NOT_FOUND=身份不存在 / NOT_MAPPED=用户存在但无激活租户
         */
        @Schema(description = "缺失原因 null / NOT_FOUND / NOT_MAPPED")
        private String missingReason;

        /**
         * 数据最后更新时间
         */
        @Schema(description = "数据最后更新时间")
        private Date sourceUpdatedAt;
    }
}
