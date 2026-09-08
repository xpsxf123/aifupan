package com.jiuyu.governance.business.performance.pojo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 开放接口 - 按视频ID批量查询视频商品（含指标）请求
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
public class OpenVideoProductQueryRequest {

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
     * 视频ID集合（爱复盘视频列表接口限制最多 500 个）
     */
    @NotEmpty(message = "视频ID集合不能为空")
    @Size(max = 500, message = "视频ID集合最多500个")
    private List<String> videoIds;

    /**
     * 过程数据步长（分钟），默认1，取值1~10。
     * <p>
     * OSS 里的商品过程数据是分钟级累计快照。步长为 N 时按 N 分钟分窗，
     * 每个窗口只取窗口内最后一条快照，值仍是累计口径（与步长1同口径，只是采样点变稀疏）。
     * </p>
     */
    @Min(value = 1, message = "步长最小为1分钟")
    @Max(value = 10, message = "步长最大为10分钟")
    private Integer step = 1;
}
