package com.jiuyu.replay.generic.vo.power.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/30 10:56
 */
@Data
public class UserAmbitionStatisticsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户意向")
    private String userAmbition;

    @Schema(description = "数值")
    private Integer num;

}
