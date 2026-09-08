package com.jiuyu.replay.generic.vo.power.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/2/4 19:16
 */
@Data
public class TeamEchartsStatisticsVo implements Serializable {
    private static final long serialVersionUID = 1L;


    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "销售ID")
    private Long salesId;

    @Schema(description = "销售名称")
    private String salesName;

    @Schema(description = "数据列表")
    private List<UserAmbitionStatisticsVo> dataList;
}
