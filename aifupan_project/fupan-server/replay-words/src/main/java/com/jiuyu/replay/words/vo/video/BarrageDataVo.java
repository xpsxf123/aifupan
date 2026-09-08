package com.jiuyu.replay.words.vo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/6 下午3:21
 */
@Data
public class BarrageDataVo {

    @Schema(description = "弹幕数据列表")
    private List<Map<String, Object>> barrageDataList;

    @Schema(description = "弹幕数量")
    private Integer totalBarrageNum;
}
