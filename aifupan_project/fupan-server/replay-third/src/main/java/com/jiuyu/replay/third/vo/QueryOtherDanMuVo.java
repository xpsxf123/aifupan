package com.jiuyu.replay.third.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/18 下午7:50
 */
@Data
@Schema(description = "其他场次弹幕返回参数")
public class QueryOtherDanMuVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "场次时间")
    private Date batchNumberDate;

    @Schema(description = "场次号")
    private String batchNumber;

    @Schema(description = "当前场次的弹幕集合")
    private List<DanMuVo> list;
}