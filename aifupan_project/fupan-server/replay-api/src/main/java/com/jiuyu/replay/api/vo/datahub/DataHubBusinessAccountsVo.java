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
 * Data Hub 业务账号响应（租户×平台汇总 + 明细）
 */
@Data
@Schema(description = "Data Hub 业务账号响应")
public class DataHubBusinessAccountsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 快照时间（实时查询时刻）
     */
    @Schema(description = "快照时间")
    private Date observedAt;

    /**
     * 每租户一项（与入参 tenantIds 一一对应）
     */
    @Schema(description = "租户业务账号列表")
    private List<Item> items = new ArrayList<>();

    /**
     * 单租户业务账号
     */
    @Data
    @Schema(description = "单租户业务账号")
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
         * 平台汇总（在册口径）
         */
        @Schema(description = "平台汇总")
        private List<PlatformCount> summary = new ArrayList<>();

        /**
         * 账号明细
         */
        @Schema(description = "账号明细")
        private List<Account> accounts = new ArrayList<>();
    }

    /**
     * 平台计数
     */
    @Data
    @Schema(description = "平台计数")
    public static class PlatformCount implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 平台 DOUYIN / KUAISHOU / WECHAT_CHANNELS / UNKNOWN
         */
        @Schema(description = "平台")
        private String platform;

        /**
         * 在册账号数（未从列表移除）
         */
        @Schema(description = "在册账号数")
        private Integer totalCount;
    }

    /**
     * 账号明细项
     */
    @Data
    @Schema(description = "账号明细项")
    public static class Account implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 业务账号ID（平台侧稳定 sec_uid）
         */
        @Schema(description = "业务账号ID（sec_uid）")
        private String businessAccountId;

        /**
         * 账号昵称
         */
        @Schema(description = "账号昵称")
        private String nickname;

        /**
         * 平台 DOUYIN / KUAISHOU / WECHAT_CHANNELS / UNKNOWN
         */
        @Schema(description = "平台")
        private String platform;

        /**
         * 账号归属类型 OWN自有 / PEER同行
         */
        @Schema(description = "账号归属类型 OWN / PEER")
        private String accountType;

        /**
         * 添加人用户ID
         */
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "添加人用户ID")
        private Long addedByUserId;

        /**
         * 添加时间
         */
        @Schema(description = "添加时间")
        private Date addedAt;

        /**
         * 是否已从列表移除
         */
        @Schema(description = "是否已从列表移除")
        private Boolean removed;

        /**
         * 移除时间（仅已移除账号）
         */
        @Schema(description = "移除时间")
        private Date removedAt;
    }
}
