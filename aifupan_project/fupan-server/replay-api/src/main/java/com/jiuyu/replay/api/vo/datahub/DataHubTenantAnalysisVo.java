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
 * Data Hub 账号分析状态响应（直播录像智能分析，租户×业务账号聚合）
 */
@Data
@Schema(description = "Data Hub 账号分析状态响应")
public class DataHubTenantAnalysisVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实际生效的统计窗口起（未传窗口时为空 = 全历史）
     */
    @Schema(description = "实际生效的统计窗口起")
    private Date windowStart;

    /**
     * 实际生效的统计窗口止
     */
    @Schema(description = "实际生效的统计窗口止")
    private Date windowEnd;

    /**
     * 每租户一项（与入参 tenantIds 一一对应）
     */
    @Schema(description = "租户分析状态列表")
    private List<Item> items = new ArrayList<>();

    /**
     * 单租户分析状态
     */
    @Data
    @Schema(description = "单租户分析状态")
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 租户ID
         */
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "租户ID")
        private Long tenantId;

        /**
         * 缺失原因：null=命中 / NOT_FOUND=租户不存在
         */
        @Schema(description = "缺失原因 null / NOT_FOUND")
        private String missingReason;

        /**
         * 按业务账号聚合的成功分析统计
         */
        @Schema(description = "按业务账号聚合的成功分析统计")
        private List<Account> accounts = new ArrayList<>();
    }

    /**
     * 账号分析聚合项
     */
    @Data
    @Schema(description = "账号分析聚合项")
    public static class Account implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 业务账号ID（sec_uid）
         */
        @Schema(description = "业务账号ID（sec_uid）")
        private String businessAccountId;

        /**
         * 窗口内成功分析次数（未传窗口 = 累计）
         */
        @Schema(description = "成功分析次数")
        private Long analysisCount;

        /**
         * 首次成功分析时间
         */
        @Schema(description = "首次成功分析时间")
        private Date firstAnalyzedAt;

        /**
         * 最近一次成功分析时间
         */
        @Schema(description = "最近一次成功分析时间")
        private Date lastAnalyzedAt;
    }
}
