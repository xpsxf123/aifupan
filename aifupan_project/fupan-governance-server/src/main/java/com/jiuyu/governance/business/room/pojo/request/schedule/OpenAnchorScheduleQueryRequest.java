package com.jiuyu.governance.business.room.pojo.request.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 开放接口 - 批量查询直播间「主播」排班请求
 * <p>
 * 供外部应用（与本服务非同一应用、不同 IP）通过 {@code @APIKey} 调用。
 * 身份信息（userId/tenantId/userType）由调用方在请求体中显式传入，
 * 不依赖网关注入；数据隔离以 tenantId 为准。
 * </p>
 *
 * @author HeHui
 * @date 2026-06-22
 */
@Getter
@Setter
public class OpenAnchorScheduleQueryRequest {

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
     * 待查询的明细集合：每项含 videoId、secUid、平台类型、开始/结束时间
     */
    @Valid
    @NotEmpty(message = "查询明细不能为空")
    private List<AnchorQueryScheduleRequest> items;
}
