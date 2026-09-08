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
 * Data Hub 订单增量对账响应
 */
@Data
@Schema(description = "Data Hub 订单增量对账响应")
public class DataHubOrderReconVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单事实列表（按 update_date, id 升序）
     */
    @Schema(description = "订单事实列表")
    private List<Item> items = new ArrayList<>();

    /**
     * 下一页游标（hasMore=false 时为空）
     */
    @Schema(description = "下一页游标")
    private String nextCursor;

    /**
     * 是否还有下一页
     */
    @Schema(description = "是否还有下一页")
    private Boolean hasMore;

    /**
     * 订单事实项
     */
    @Data
    @Schema(description = "订单事实项")
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 订单号（幂等去重键，与事件推送 orderId 一致）
         */
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "订单号")
        private Long orderId;

        /**
         * 下单用户ID
         */
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "下单用户ID")
        private Long userId;

        /**
         * 归属租户ID（无法归属时为空）
         */
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "归属租户ID")
        private Long tenantId;

        /**
         * 租户归属方式 SNAPSHOT业务时点快照 / BACKFILLED按当前关系回填 / UNKNOWN无法归属
         */
        @Schema(description = "租户归属方式 SNAPSHOT / BACKFILLED / UNKNOWN")
        private String tenantAttribution;

        /**
         * 订单状态：UNPAID / NOT_STARTED / ACTIVE / EXPIRED / REFUNDED /
         * ENDED_BY_UPGRADE / CLOSED_TIMEOUT / FROZEN / CANCELED_MANUAL
         */
        @Schema(description = "订单状态")
        private String status;

        /**
         * 订单类型：FREE / UPGRADE / FREE_TO_PAID / RENEWAL / INCREMENT /
         * VERSION_ACTIVITY / EDIT / COMMODITY_ACTIVITY
         */
        @Schema(description = "订单类型")
        private String orderType;

        /**
         * 商品类型：INCREMENT / NORMAL_VERSION / ACTIVITY_VERSION / INVITE_ACTIVITY
         */
        @Schema(description = "商品类型")
        private String commodityType;

        /**
         * 订单来源：NORMAL正常下单 / MANUAL后台手动添加 / INVITE_REWARD邀请用户成功 / INVITE_CODE邀请码赠送
         */
        @Schema(description = "订单来源")
        private String source;

        /**
         * 是否试用订单
         */
        @Schema(description = "是否试用订单")
        private Boolean isTrial;

        /**
         * 实付金额（分）
         */
        @Schema(description = "实付金额（分）")
        private Integer priceCent;

        /**
         * 支付成功时间（无支付为空）
         */
        @Schema(description = "支付成功时间")
        private Date paidAt;

        /**
         * 服务生效时间
         */
        @Schema(description = "服务生效时间")
        private Date serviceStartAt;

        /**
         * 服务到期时间
         */
        @Schema(description = "服务到期时间")
        private Date serviceEndAt;

        /**
         * 订单创建时间
         */
        @Schema(description = "订单创建时间")
        private Date createdAt;

        /**
         * 订单最后更新时间（增量与游标基准）
         */
        @Schema(description = "订单最后更新时间")
        private Date updatedAt;

        /**
         * 商品名称
         */
        @Schema(description = "商品名称")
        private String commodityName;

        /**
         * 订单标题
         */
        @Schema(description = "订单标题")
        private String title;
    }
}
