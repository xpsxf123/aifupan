package com.jiuyu.governance.business.performance.pojo.request;

import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.plugins.webmvc.serializer.EnumDesc;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 开放接口 - 按「平台 + 直播批次号集合」批量查询商品存在性请求
 * <p>
 * 供外部应用（非同一应用、不同 IP）通过 {@code @APIKey} 调用。
 * 支持多平台：{@code items} 中每项是「一个平台 + 该平台下的多个批次号」。
 * 身份字段（userId/tenantId/userType）由调用方在请求体中显式传入；数据隔离以 tenantId 为准。
 * </p>
 *
 * @author HeHui
 * @date 2026-08-06
 */
@Getter
@Setter
public class OpenBatchProductExistsRequest {

    /**
     * 用户ID（调用方传入，作为身份透传）
     */
    @NotNull(message = "userId不能为空")
    private Long userId;

    /**
     * 租户ID（数据隔离边界）
     */
    @NotNull(message = "tenantId不能为空")
    private Long tenantId;

    /**
     * 用户类型（0主账号/1管理员/2子账号，按调用方语义透传）
     */
    private Integer userType;

    /**
     * 待查询的平台分组集合：每项含平台类型 + 该平台下的批次号集合
     */
    @Valid
    @NotEmpty(message = "查询明细不能为空")
    private List<PlatformBatchItem> items;

    /**
     * 平台分组：一个平台 + 多个直播批次号
     */
    @Getter
    @Setter
    public static class PlatformBatchItem {

        /**
         * 平台类型：0-抖音，1-快手，2-视频号
         */
        @NotNull(message = "平台类型不能为空")
        @EnumDesc(LivePlatformType.class)
        private Integer platform;

        /**
         * 该平台下的直播批次号集合
         */
        @NotEmpty(message = "批次号集合不能为空")
        private List<String> batchNumbers;
    }
}
