package com.jiuyu.replay.words.constant;

import lombok.Data;

@Data
public class SystemKeyConstant {

    /**
     * 生成主播关键词时的视频时长要求（秒）
     */
    public static final String generateAnchorKeywordVideoDuration = "generate_anchor_keyword_video_duration";
    /**
     * AI纠正主播行业的视频时长要求（秒）
     */
    public static final String correctAnchorTradeVideoDuration = "correct_anchor_trade_video_duration";
}
