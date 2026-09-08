package com.jiuyu.replay.words.bo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台管理-行业热榜查询BO
 *
 * @author RayChou
 * @date 2025-11-05
 * @description 后台管理系统行业热榜分页查询请求对象
 */
@Data
@Schema(description = "后台管理-行业热榜查询BO")
public class TradeRankAdminQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数", example = "10")
    private Integer limit = 10;

    /**
     * 行业ID（可选，不传则查询全部）
     */
    @Schema(description = "行业ID（可选，不传则查询全部）", example = "1001")
    private Long tradeId;

    /**
     * 主播抖音账号（精确筛选）
     */
    @Schema(description = "主播抖音账号（精确筛选）", example = "douyin123")
    private String uniqueId;

    /**
     * 主播名称（模糊搜索）
     */
    @Schema(description = "主播名称（模糊搜索）", example = "张三")
    private String anchorName;

    /**
     * 热度开始值
     */
    @Schema(description = "热度开始值", example = "1000")
    private Integer heatStart;

    /**
     * 热度结束值
     */
    @Schema(description = "热度结束值", example = "5000")
    private Integer heatEnd;

    /**
     * 更新时间开始值
     */
    @Schema(description = "更新时间开始值", example = "2025-10-01 00:00:00")
    private LocalDateTime updateTimeStart;

    /**
     * 更新时间结束值
     */
    @Schema(description = "更新时间结束值", example = "2025-10-31 23:59:59")
    private LocalDateTime updateTimeEnd;

    /**
     * 排序字段（followerCount:粉丝量, liveCount:直播场次, liveAverageUser:平均场观, totalAmount:总销售额, accountHeat:热度）
     */
    @Schema(description = "排序字段（followerCount:粉丝量, liveCount:直播场次, liveAverageUser:平均场观, totalAmount:总销售额, accountHeat:热度）",
            example = "accountHeat")
    private String orderBy;

    /**
     * 排序方式（ASC:升序, DESC:降序）
     */
    @Schema(description = "排序方式（ASC:升序, DESC:降序）", example = "DESC")
    private String orderDirection;

    /**
     * 是否在库
     */
    private Boolean inStock;

    /**
     * 是否上榜
     */
    private Boolean upRanking;

    /**
     * 数据来源类型 1-系统主播相似达人，2-后台录入，3-第三方榜单
     */
    private List<Integer> sourceTypes;

    /**
     * 直播关键词
     */
    private String liveKeyword;

    /**
     * 是否有效 决定是否需要过滤 30天内更新过的数据
     */
    private Boolean effective = true;

    /**
     * 采集状态：0-待采集，1-采集中（仅针对系统主播相似达人），2-采集完成
     */
    private List<Integer> collectStatus;
}

