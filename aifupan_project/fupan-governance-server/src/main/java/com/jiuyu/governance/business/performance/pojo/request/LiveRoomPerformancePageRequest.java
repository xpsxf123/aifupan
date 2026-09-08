package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.framework.shandard.PageRequest;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.time.LocalDate;
import java.util.List;

/**
 * 各直播间业绩数据分页查询请求
 * <p>
 * 使用场景：业绩汇总-各直播间分页列表，分公司业绩汇总详情-直播间数据，部门业绩汇总详情-直播间数据，小组业绩汇总详情-直播间数据
 * 主表为直播间，统计对应的业绩数据，业绩来源表为session_performance
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
public class LiveRoomPerformancePageRequest extends PageRequest implements DataPermissionsRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 直播间名称（模糊查询，可不传）
     * 对应 live_room.anchor_name 字段
     */
    private String name;

    /**
     * 分公司ID（精确匹配，可不传）
     * 传入时仅查询该分公司下的直播间业绩
     */
    private Long companyId;

    /**
     * 部门ID（精确匹配，可不传）
     * 传入时仅查询该部门下的直播间业绩
     */
    private Long deptId;

    /**
     * 小组ID（精确匹配，可不传）
     * 传入时仅查询该小组下的直播间业绩
     */
    private Long teamId;

    /**
     * 开始时间（必传，格式：yyyy-MM-dd）
     * 查询 stats_date >= startDate 的数据
     */
    @NotNull(message = "开始时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 结束时间（必传，格式：yyyy-MM-dd）
     * 查询 stats_date <= endDate 的数据
     */
    @NotNull(message = "结束时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 直播间ID列表（数据权限过滤，由框架自动注入）
     */
    @JsonIgnore
    private List<Long> liveRoomIds;

    /**
     * 租户ID（由框架自动注入）
     */
    @JsonIgnore
    private Long tenantId;

    /**
     * 排序字段（viewCount/salesRevenue/refund/netSales/investment），默认按销售额降序
     */
    private String sortField = "salesRevenue";

    /**
     * 排序方向（ASC/DESC），默认 DESC
     */
    private String sortOrder = "DESC";

    @Override
    public List<String> dataTypes() {
        return List.of(OauthConstant.LIVE_ROOM);
    }

    @Override
    public void initCurrentUserId(Long currentUserId) {
    }

    @Override
    public List<Long> findDataIds(String dataType) {
        if (dataType.equals(OauthConstant.LIVE_ROOM)) {
            return liveRoomIds;
        }
        return null;
    }

    @Override
    public void toDataIds(String dataType, List<Long> dataIds) {
        if (dataType.equals(OauthConstant.LIVE_ROOM)) {
            liveRoomIds = dataIds;
        }
    }
}
