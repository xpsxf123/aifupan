package com.jiuyu.governance.business.performance.pojo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开放接口 - 租户下商品搜索请求
 * <p>
 * 供外部应用（非同一应用、不同 IP）通过 {@code @APIKey} 调用。
 * 身份字段（userId/tenantId/userType）由调用方在请求体中显式传入；数据隔离以 tenantId 为准。
 * </p>
 *
 * @author HeHui
 * @date 2026-08-06
 */
@Getter
@Setter
public class OpenProductSearchRequest {

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
     *  商品ID
     */
    private List<String> productIds;

    /**
     * 商品名称搜索关键字（模糊匹配，不传则不过滤）
     */
    private String name;

    /**
     * 返回条数，默认100，最大3000
     */
    @Min(value = 1, message = "返回条数最小为1")
    @Max(value = 3000, message = "返回条数最大为3000")
    private Integer limit = 100;

    /**
     * 批次号查询起始时间（格式：yyyy-MM-dd HH:mm:ss）。
     * 与 endTime 一起限定 batchNumber 的时间范围，跨度不得超过一年；不传则取近一年。
     */
    private LocalDateTime startTime;

    /**
     * 批次号查询截止时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    private LocalDateTime endTime;
}
