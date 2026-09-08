package com.jiuyu.replay.words.vo.anchor;

import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI复盘主播列表信息")
public class AnchorRecordListVo extends AnchorUrlUserVo {

    /**
     * 今日录制数量
     */
    @Schema(description = "今日录制数量")
    private Integer recordTodayTotal;
    /**
     * 总录制数量
     */
    @Schema(description = "总录制数量")
    private Integer recordTotal;
}
