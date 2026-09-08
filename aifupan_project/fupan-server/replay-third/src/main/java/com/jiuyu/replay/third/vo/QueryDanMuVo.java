package com.jiuyu.replay.third.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/16 下午3:57
 */
@Data
@Schema(description = "弹幕返回参数")
public class QueryDanMuVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否有下一页")
    private Boolean nextHash;

    @Schema(description = "是否有上一页")
    private Boolean previousHash;

    @Schema(description = "重要弹幕生成状态：0未获取，1获取中，2获取成功，3获取失败")
    private Integer importantBarrageStatus;

    @Schema(description = "弹幕数据")
    private List<DanMuVo> list;
}
