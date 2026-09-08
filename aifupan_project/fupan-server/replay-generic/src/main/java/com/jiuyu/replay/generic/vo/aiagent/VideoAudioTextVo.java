package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 短视频原文音频转文字内容项。
 *
 * @author fupan-server
 */
@Data
public class VideoAudioTextVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 视频 id（tb_video_info.id）
     */
    private Long videoId;

    /**
     * 视频文件 hash
     */
    private String videoHash;

    /**
     * 原文音频转文字内容（未优化）；Mongo 无内容时为 null
     */
    private String originalAudioContent;

    /**
     * AI 优化后音频转文字内容；Mongo 无内容时为 null
     */
    private String audioContent;
}
