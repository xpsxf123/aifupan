
package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class SyncVideoAnalysisBo {

    /**
     * 视频uuid
     */
    @Schema(description = "视频uuid")
    private String videoId;
    /**
     * 行业id
     */
    @Schema(description = "行业id")
    private Long tradeId;
    /**
     * 分析数据
     */
    @Schema(description = "分析数据")
    private List<SentenceMarkVo> sentenceMarkVoList;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
}
