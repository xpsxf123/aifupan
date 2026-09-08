package com.jiuyu.replay.api.bo.crm;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * CRM 订单事件出站请求体
 */
@Data
@Schema(description = "CRM 订单事件出站请求体")
public class CrmOrderEventPushBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "事件ID")
    private String eventId;
    @Schema(description = "事件类型")
    private String eventType;
    @Schema(description = "事件发生时间(ISO_OFFSET)")
    private String occurredAt;
    @Schema(description = "客户信息")
    private Customer customer;
    @Schema(description = "销售信息")
    private Sales sales;
    @Schema(description = "订单信息")
    private OrderPayload order;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "客户信息")
    public static class Customer implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "客户手机号")
        private String phone;
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "客户ID(user_id)")
        private Long userId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "销售信息")
    public static class Sales implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "销售手机号")
        private String salesPhone;
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "销售ID")
        private Long salesId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单信息")
    public static class OrderPayload implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "订单ID(order_id)")
        private Long orderId;
        @Schema(description = "订单类型")
        private String orderType;
        @Schema(description = "商品类型")
        private String commodityType;
        @Schema(description = "是否试用单")
        private Boolean isTrial;
        @Schema(description = "订单状态")
        private String status;
        @Schema(description = "订单开始时间(ISO_OFFSET)")
        private String startTime;
        @Schema(description = "订单结束时间(ISO_OFFSET)")
        private String endTime;
        @Schema(description = "订单金额(分)")
        private Integer priceCent;
    }
}
