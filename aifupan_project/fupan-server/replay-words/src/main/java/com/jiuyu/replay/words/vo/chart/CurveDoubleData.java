package com.jiuyu.replay.words.vo.chart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/8/7 上午9:48
 */
@Data
@Schema(description = "曲线数据项-Double")
public class CurveDoubleData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long dateTime;

    private Long dateTimeNew;

    private Double valueNum;
}
