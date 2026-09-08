package com.jiuyu.replay.generic.vo.words.video;

import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 子账号录制视频列表VO
 *
 * @author AI Assistant
 */
@Data
@Schema(description = "子账号录制视频列表VO")
public class SubUserVideoListVo extends AnchorVideoInfoVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主播信息
     */
    @Schema(description = "主播信息")
    private AnchorUrlInfoVo anchorUrlInfoVo;

    /**
     * 销售额区间起始值（单位：元）
     */
    @Schema(description = "销售额区间起始值（单位：元）")
    private Integer volumeStart;

    /**
     * 销售额区间结束值（单位：元）
     */
    @Schema(description = "销售额区间结束值（单位：元）")
    private Integer volumeEnd;

    /**
     * 总观看人次
     */
    @Schema(description = "总观看人次")
    private Integer totalWatchNum;

    /**
     * 是否有小结 0：没有 1：有
     */
    @Schema(description = "是否有小结 0：没有 1：有")
    private Integer hasNotes;
}
