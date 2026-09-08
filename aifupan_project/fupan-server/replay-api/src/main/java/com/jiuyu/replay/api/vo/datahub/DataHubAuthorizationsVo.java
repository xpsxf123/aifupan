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
 * Data Hub 授权状态响应（粒度：租户×成员×业务账号×授权类型）
 */
@Data
@Schema(description = "Data Hub 授权状态响应")
public class DataHubAuthorizationsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 每租户一项（与入参 tenantIds 一一对应）
     */
    @Schema(description = "租户授权列表")
    private List<Item> items = new ArrayList<>();

    /**
     * 单租户授权状态
     */
    @Data
    @Schema(description = "单租户授权状态")
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
         * 授权状态明细（每条 = 一个业务账号的一类授权）
         */
        @Schema(description = "授权状态明细")
        private List<Authorization> authorizations = new ArrayList<>();
    }

    /**
     * 授权状态项
     */
    @Data
    @Schema(description = "授权状态项")
    public static class Authorization implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 业务账号ID（sec_uid）
         */
        @Schema(description = "业务账号ID（sec_uid）")
        private String businessAccountId;

        /**
         * 授权记录归属成员用户ID
         */
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "归属成员用户ID")
        private Long operatorUserId;

        /**
         * 授权类型 JULIANG_BAIYING / QIANCHUAN / WECHAT_CHANNELS / DOUYIN_LAIKE
         */
        @Schema(description = "授权类型")
        private String authType;

        /**
         * 授权状态：NOT_AUTHORIZED / AUTHORIZED / EXPIRED / FAILED / AUTHORIZING / ACCOUNT_MISMATCH；
         * WECHAT_CHANNELS 专有：CANCELED_BY_WECHAT / CANCELED_BY_USER
         */
        @Schema(description = "授权状态")
        private String status;

        /**
         * 状态变更时间（WECHAT_CHANNELS 无此数据，恒为空）
         */
        @Schema(description = "状态变更时间")
        private Date statusUpdatedAt;
    }
}
